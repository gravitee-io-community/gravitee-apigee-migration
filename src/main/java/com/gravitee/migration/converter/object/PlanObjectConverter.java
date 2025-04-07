package com.gravitee.migration.converter.object;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.registry.PolicyConverterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.gravitee.migration.util.ConditionConverter.constructCondition;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class PlanObjectConverter {

    private final XPath xPath;
    private final Map<String, ObjectNode> apiGeePoliciesMap = new HashMap<>();
    private final PolicyConverterRegistry policyConverterRegistry;

    /**
     * Creates a plan node in the Gravitee API configuration.
     *
     * @param graviteeNode    The api node in the Gravitee configuration.
     * @param planName        The name of the plan to be created, extracted from the API name
     * @param apiGeePolicies  The list of policies in ApiGee.
     * @param targetEndpoints The list of target endpoints in ApiGee.
     * @param proxyXml        The proxy XML document.
     */
    public void createPlan(ObjectNode graviteeNode, String planName, List<Document> apiGeePolicies, List<Document> targetEndpoints, Document proxyXml) throws XPathExpressionException {
        var plansArray = graviteeNode.putArray(PLANS);
        // For now, we are creating only 1 plan per API
        var planNode = buildPlanNode(planName, plansArray);

        // Create the flows array inside the plan node
        var flowsArray = planNode.putArray(FLOWS);

        // Map flow from the proxy XML located in the proxies folder
        buildFlowsFromProxyXml(apiGeePolicies, proxyXml, targetEndpoints, flowsArray);
    }

    private void buildFlowsFromProxyXml(List<Document> apiGeePolicies, Document proxyXml, List<Document> targetEndpoints, ArrayNode flowsArray) throws XPathExpressionException {
        // Extract preflows, flows, and postflows from the proxy XML
        var preFlowNodes = (NodeList) xPath.evaluate("/*/PreFlow", proxyXml, XPathConstants.NODESET);
        var flows = (NodeList) xPath.evaluate("/*/Flows/Flow", proxyXml, XPathConstants.NODESET);
        var postFlows = (NodeList) xPath.evaluate("/*/PostFlow", proxyXml, XPathConstants.NODESET);

        // Create the preflow first that will be executed before all other flows
        buildFlows(apiGeePolicies, preFlowNodes, flowsArray);
        // Create the flows that will be executed in the order they are defined based on conditions
        buildFlows(apiGeePolicies, flows, flowsArray);
        // Map the flows from the target endpoints
        buildRouteRules(apiGeePolicies, proxyXml, targetEndpoints, flowsArray);
        // Create the postflow that will be executed after all other flows
        buildFlows(apiGeePolicies, postFlows, flowsArray);
    }

    private void buildFlows(List<Document> apiGeePolicies, NodeList flows, ArrayNode flowsArray) throws XPathExpressionException {
        // Check if flows are present
        if (flows != null && flows.getLength() > 0) {

            // Iterate through all the flows
            for (int i = 0; i < flows.getLength(); i++) {
                var flow = flows.item(i);
                buildFlow(apiGeePolicies, flow, flowsArray);
            }
        }
    }

    private void buildFlow(List<Document> apiGeePolicies, Node flow, ArrayNode flowsArray) throws XPathExpressionException {
        // Extract the request steps from the current flow
        var requestStepNodes = (NodeList) xPath.evaluate("Request/Step", flow, XPathConstants.NODESET);
        // Extract the response steps from the current flow
        var responseStepNodes = (NodeList) xPath.evaluate("Response/Step", flow, XPathConstants.NODESET);

        // Extract the condition from the current flow
        var flowCondition = xPath.evaluate("Condition", flow);
        // Extract the name from the current flow
        var flowName = xPath.evaluate("@name", flow);
        var flowObject = createFlowDefinition(flowName, flowCondition, flowsArray);

        var requestArray = flowObject.putArray(REQUEST);
        var responseArray = flowObject.putArray(RESPONSE);

        // Create policies inside the request and response arrays inside the flow object
        applyPoliciesToFlowNodes(requestStepNodes, apiGeePolicies, requestArray);
        applyPoliciesToFlowNodes(responseStepNodes, apiGeePolicies, responseArray);
    }


    private ObjectNode createFlowDefinition(String flowName, String flowCondition, ArrayNode flowsArray) {
        var flowNode = flowsArray.addObject();

        flowNode.put(ID, UUID.randomUUID().toString());
        flowNode.put(NAME, flowName);
        // TODO: ASK IF FLOW SHOULD BE ENABLED OR DISABLED BY DEFAULT
        flowNode.put(ENABLED, true);

        // Create selectors array inside the flow node
        var selectors = flowNode.putArray(SELECTORS);
        var selectorsObject = selectors.addObject();
        // Populate selectors based on the condition input
        constructCondition(flowCondition, selectorsObject);

        return flowNode;
    }

    private void buildRouteRules(List<Document> apiGeePolicies, Document proxyXml, List<Document> targetEndpoints, ArrayNode flowsArray) throws XPathExpressionException {
        // Extract all route rules from the proxyXml
        var routeRules = (NodeList) xPath.evaluate("/ProxyEndpoint/RouteRule", proxyXml, XPathConstants.NODESET);

        // Iterate through each route rule
        for (int i = 0; i < routeRules.getLength(); i++) {
            var routeRule = routeRules.item(i);
            // Extract the target endpoint name and condition from the route rule
            var routeRuleTargetEndpoint = xPath.evaluate("TargetEndpoint", routeRule);
            // TODO: ADD CONVERTER
            var condition = xPath.evaluate("Condition", routeRule);

            // Find the corresponding target endpoint XML document
            for (Document targetEndpoint : targetEndpoints) {
                // Extract the target endpoint name from the target endpoint XML
                var targetEndpointName = xPath.evaluate("TargetEndpoint/@name", targetEndpoint);
                if (routeRuleTargetEndpoint.equals(targetEndpointName)) {
                    buildTargetEndpointFlows(apiGeePolicies, targetEndpoint, flowsArray, condition);
                    break;
                }
            }
        }
    }

    private void buildTargetEndpointFlows(List<Document> apiGeePolicies, Document targetEndpoint, ArrayNode flowsArray, String condition) throws XPathExpressionException {
        // Extract the target endpoint name from the target endpoint XML - used for creating the policy name
        var targetEndpointName = xPath.evaluate("/TargetEndpoint/@name", targetEndpoint);

        // Extract the preflow, postflow, and routing policies from the target endpoint XML
        var preFlowRequestNodes = (NodeList) xPath.evaluate("/TargetEndpoint/PreFlow/Request/Step", targetEndpoint, XPathConstants.NODESET);
        var preFlowResponse = (NodeList) xPath.evaluate("/TargetEndpoint/PreFlow/Response/Step", targetEndpoint, XPathConstants.NODESET);
        var postFlowRequest = (NodeList) xPath.evaluate("/TargetEndpoint/PreFlow/Request/Step", targetEndpoint, XPathConstants.NODESET);
        var postFlowsResponse = (NodeList) xPath.evaluate("/TargetEndpoint/PostFlow/Response/Step", targetEndpoint, XPathConstants.NODESET);

        // Create the flow object for the target endpoint
        var flowObject = createFlowDefinition(targetEndpointName.concat("-flow"), condition, flowsArray);

        // Create request and response arrays inside the flow object - for each target endpoint both preflow and postflow are combined into one flow
        var requestArray = flowObject.putArray(REQUEST);
        var responseArray = flowObject.putArray(RESPONSE);

        // Add preflow request steps to the request phase
        applyPoliciesToFlowNodes(preFlowRequestNodes, apiGeePolicies, requestArray);
        // Add postflow request steps to the request phase
        applyPoliciesToFlowNodes(postFlowRequest, apiGeePolicies, responseArray);
        // Add routing policy to route the request to the target endpoint
        buildRoutingPolicy(targetEndpoint, requestArray);
        // Add preflow response steps to the response phase
        applyPoliciesToFlowNodes(preFlowResponse, apiGeePolicies, responseArray);
        // Add postflow response steps to the response phase
        applyPoliciesToFlowNodes(postFlowsResponse, apiGeePolicies, responseArray);
    }

    public void applyPoliciesToFlowNodes(NodeList stepNodes, List<Document> apiGeePolicies, ArrayNode objectNodes) throws XPathExpressionException {
        if (stepNodes != null && stepNodes.getLength() > 0) {
            for (int j = 0; j < stepNodes.getLength(); j++) {
                var stepNode = stepNodes.item(j);

                findAndApplyMatchingPolicy(stepNode, apiGeePolicies, objectNodes);
            }
        }
    }

    private void findAndApplyMatchingPolicy(Node stepNode, List<Document> apiGeePolicies, ArrayNode objectNodes) throws XPathExpressionException {
        var stepName = xPath.evaluate("Name", stepNode);

        for (Document apiGeePolicy : apiGeePolicies) {
            var displayName = xPath.evaluate("/*/DisplayName", apiGeePolicy);
            if (stepName.equals(displayName)) {
                applyPolicyConverter(stepNode, apiGeePolicy, objectNodes);
            }
        }
    }

    private void applyPolicyConverter(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray) throws XPathExpressionException {
        var rootElement = (Node) xPath.evaluate("/*", apiGeePolicy, XPathConstants.NODE);
        var policyType = rootElement.getNodeName();

        try {
            var converter = policyConverterRegistry.getConverter(policyType);
            converter.convert(stepNode, apiGeePolicy, scopeArray);
        } catch (Exception e) {
            // if policy cannot be mapped create custom one to inform client
            log.warn(e.getMessage());
        }
    }

    private void buildRoutingPolicy(Document targetEndpoint, ArrayNode requestArray) throws XPathExpressionException {
        var routingPolicy = requestArray.addObject();

        routingPolicy.put(NAME, "Routing Policy");
        routingPolicy.put(ENABLED, true);
        routingPolicy.put(ASYNC, false);
        routingPolicy.put(POLICY, "dynamic-routing");

        var routingPolicyConf = routingPolicy.putObject(CONFIGURATION);
        var rulesArray = routingPolicyConf.putArray(RULES);
        var ruleObject = rulesArray.addObject();
        ruleObject.put(PATTERN, "/*");

        var targetUrl = constructUrl(targetEndpoint);
        ruleObject.put(URL, targetUrl);
    }

    private String constructUrl(Document targetEndpoint) throws XPathExpressionException {
        var targetEndpointName = xPath.evaluate("/TargetEndpoint/@name", targetEndpoint);

        return "{#endpoints['" + targetEndpointName + "']}";
    }

    /**
     * Creates the plan and populates the plan object inside the plans array
     *
     * @param planName   The name of the plan to be created, extracted from the API name
     * @param plansArray The array of plans.
     * @return The created plan node.
     */
    private ObjectNode buildPlanNode(String planName, ArrayNode plansArray) {
        var plansNode = plansArray.addObject();

        // Hardcoded values
        plansNode.put(DEFINITION_VERSION, V4);
        plansNode.put(ID, UUID.randomUUID().toString());
        plansNode.put(DESCRIPTION, planName.concat("-plan"));
        plansNode.put(STATUS, "PUBLISHED");
        plansNode.put(VALIDATION, "MANUAL");

        // Name extracted from the API name
        plansNode.put(NAME, planName);

        buildPlansSecurity(plansNode);

        return plansNode;
    }

    /**
     * Constructs the security node inside the plan object
     * Hardcoded values
     */
    private void buildPlansSecurity(ObjectNode plansNode) {
        // Hardcoded values
        var securityNode = plansNode.putObject(SECURITY);
        securityNode.put(TYPE, API_KEY);
        securityNode.putObject(CONFIGURATION);
    }
}