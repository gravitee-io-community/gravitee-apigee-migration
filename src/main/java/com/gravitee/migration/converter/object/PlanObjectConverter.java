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
import java.util.List;
import java.util.UUID;

import static com.gravitee.migration.util.ConditionConverter.constructCondition;
import static com.gravitee.migration.util.StringUtils.constructEndpointsUrl;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.LOOKUP_CACHE;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.POPULATE_CACHE;

/**
 * This class is responsible for creating plans the Gravitee API configuration.
 * It handles the creation of plans, flows, and policies within the Gravitee API configuration.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanObjectConverter {

    private final XPath xPath;
    private final PolicyConverterRegistry policyConverterRegistry;
    private final ApiObjectConverter apiObjectConverter;

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
        // We are creating only 1 plan per API
        var planNode = buildPlanNode(planName, plansArray);

        // Create the flows array inside the plan node
        var flowsArray = planNode.putArray(FLOWS);

        // Map flows from the proxy XML located in the proxies folder
        buildFlowsFromProxyXml(apiGeePolicies, proxyXml, targetEndpoints, flowsArray);
    }

    private void buildFlowsFromProxyXml(List<Document> apiGeePolicies, Document proxyXml, List<Document> targetEndpoints, ArrayNode flowsArray) throws XPathExpressionException {
        // Add PreFlow Request steps to the request array
        buildPreOrPostFlowSteps("/*/PreFlow/Request/Step", REQUEST, "PreFlow-Request", apiGeePolicies, proxyXml, flowsArray);

        // Add Conditional flows to the flows array
        buildConditionalFlows(proxyXml, flowsArray, apiGeePolicies, targetEndpoints);

        // Add PreFlow Response steps to the response array
        buildPreOrPostFlowSteps("/*/PostFlow/Response/Step", RESPONSE, "PostFlow-Response", apiGeePolicies, proxyXml, flowsArray);
    }

    private void buildPreOrPostFlowSteps(String xpathExpression, String phase, String name, List<Document> apiGeePolicies, Document proxyXml, ArrayNode flowsArray) throws XPathExpressionException {
        var flowNodes = (NodeList) xPath.evaluate(xpathExpression, proxyXml, XPathConstants.NODESET);
        // PreFlow and PostFlow have no conditions - they are always executed
        buildProxyFlow(flowNodes, flowsArray, phase, name, apiGeePolicies, null);
    }

    private void buildConditionalFlows(Document proxyXml, ArrayNode flowsArray, List<Document> apiGeePolicies, List<Document> targetEndpoints) throws XPathExpressionException {
        // Extract all conditional flows from the proxyXml
        var conditionalFlows = (NodeList) xPath.evaluate("/*/Flows/Flow", proxyXml, XPathConstants.NODESET);

        // For each flow add the request steps to the flows array
        processConditionalFlowSteps(conditionalFlows, "Request/Step", REQUEST, "-Request", apiGeePolicies, flowsArray);

        // Add PostFlow Request steps to the request array
        buildPreOrPostFlowSteps("/*/PostFlow/Request/Step", REQUEST, "PostFlow-Request", apiGeePolicies, proxyXml, flowsArray);

        // Build routing rules
        buildRouteRules(apiGeePolicies, proxyXml, targetEndpoints, flowsArray);

        // Add PreFlow Response steps to the response array
        buildPreOrPostFlowSteps("/*/PreFlow/Response/Step", RESPONSE, "PreFlow-Response", apiGeePolicies, proxyXml, flowsArray);

        // For each flow add the response steps to the flows array
        processConditionalFlowSteps(conditionalFlows, "Response/Step", RESPONSE, "-Response", apiGeePolicies, flowsArray);
    }

    private void processConditionalFlowSteps(NodeList conditionalFlows, String stepXPath, String phase, String suffix, List<Document> apiGeePolicies, ArrayNode flowsArray) throws XPathExpressionException {
        for (int i = 0; i < conditionalFlows.getLength(); i++) {
            var conditionalFlow = conditionalFlows.item(i);
            var conditionalFlowName = xPath.evaluate("@name", conditionalFlow);
            var conditionalFlowCondition = xPath.evaluate("Condition", conditionalFlow);

            var flowNodes = (NodeList) xPath.evaluate(stepXPath, conditionalFlow, XPathConstants.NODESET);
            buildProxyFlow(flowNodes, flowsArray, phase, conditionalFlowName.concat(suffix), apiGeePolicies, conditionalFlowCondition);
        }
    }

    private void buildProxyFlow(NodeList steps, ArrayNode flowsArray, String phase, String name, List<Document> apiGeePolicies, String condition) throws XPathExpressionException {
        if (steps != null && steps.getLength() > 0) {

            // Create the flow object
            var flowObject = createFlowDefinition(name, condition, flowsArray);
            // Create the request or response array inside the flow object
            var scopeArray = flowObject.putArray(phase);

            // Process the steps inside the flow and map them to the corresponding policies
            for (int i = 0; i < steps.getLength(); i++) {
                var stepNode = steps.item(i);
                findAndApplyMatchingPolicy(stepNode, apiGeePolicies, scopeArray, phase);
            }
        }
    }

    private ObjectNode createFlowDefinition(String flowName, String flowCondition, ArrayNode flowsArray) {
        var flowNode = flowsArray.addObject();
        flowNode.put(ID, UUID.randomUUID().toString());
        flowNode.put(NAME, flowName);
        flowNode.put(ENABLED, true);

        // Create selectors array inside the flow node
        var selectors = flowNode.putArray(SELECTORS);
        var selectorsObject = selectors.addObject();
        // Populate selectors based on the condition input
        constructCondition(flowCondition, selectorsObject);

        return flowNode;
    }

    private void findAndApplyMatchingPolicy(Node stepNode, List<Document> apiGeePolicies, ArrayNode objectNodes, String scope) throws XPathExpressionException {
        // Extract the policy name from the step node
        var stepName = xPath.evaluate("Name", stepNode);

        for (Document apiGeePolicy : apiGeePolicies) {
            // Extract the display name of the policy
            var displayName = xPath.evaluate("/*/DisplayName", apiGeePolicy);
            // Check if the step name matches the display name of the policy
            if (stepName.equals(displayName)) {
                applyPolicyConverter(stepNode, apiGeePolicy, objectNodes, scope);
            }
        }
    }

    private void applyPolicyConverter(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws XPathExpressionException {
        // Extract the root element name of the policy
        var rootElement = (Node) xPath.evaluate("/*", apiGeePolicy, XPathConstants.NODE);
        var policyType = rootElement.getNodeName();

        // If it is a LookupCache or PopulateCache policy, build the cache into the resources
        if (policyType.equals(LOOKUP_CACHE) || policyType.equals(POPULATE_CACHE)) {
            apiObjectConverter.buildResources(apiGeePolicy);
        }

        // Check if a converter exists for the policy type
        try {
            // Get the converter for the policy type and apply it
            var converter = policyConverterRegistry.getConverter(policyType);
            converter.convert(stepNode, apiGeePolicy, scopeArray, scope);
        } catch (Exception e) {
            // if policy cannot be mapped create custom one to inform client
            log.warn(e.getMessage());
        }
    }

    private void buildRouteRules(List<Document> apiGeePolicies, Document proxyXml, List<Document> targetEndpoints, ArrayNode flowsArray) throws XPathExpressionException {
        // Extract all route rules from the proxyXml
        NodeList routeRules = (NodeList) xPath.evaluate("/ProxyEndpoint/RouteRule", proxyXml, XPathConstants.NODESET);

        // Process each route rule
        for (int i = 0; i < routeRules.getLength(); i++) {
            Node routeRule = routeRules.item(i);
            processRouteRule(routeRule, apiGeePolicies, targetEndpoints, flowsArray);
        }
    }

    private void processRouteRule(Node routeRule, List<Document> apiGeePolicies, List<Document> targetEndpoints, ArrayNode flowsArray) throws XPathExpressionException {
        // Extract the target endpoint name and condition
        String routeRuleTargetEndpoint = xPath.evaluate("TargetEndpoint", routeRule);
        String condition = xPath.evaluate("Condition", routeRule);

        // Find the matching target endpoint
        Document matchingTargetEndpoint = findMatchingTargetEndpoint(routeRuleTargetEndpoint, targetEndpoints);
        if (matchingTargetEndpoint != null) {
            // Build the flows for the matching target endpoint
            buildTargetEndpointFlows(apiGeePolicies, matchingTargetEndpoint, flowsArray, condition);
        }
    }

    private Document findMatchingTargetEndpoint(String routeRuleTargetEndpoint, List<Document> targetEndpoints) throws XPathExpressionException {
        // Iterate through the target endpoints to find a match
        for (Document targetEndpoint : targetEndpoints) {
            // Extract target endpoint name
            String targetEndpointName = xPath.evaluate("TargetEndpoint/@name", targetEndpoint);
            if (routeRuleTargetEndpoint.equals(targetEndpointName)) {
                return targetEndpoint;
            }
        }
        return null;
    }

    private void buildTargetEndpointFlows(List<Document> apiGeePolicies, Document targetEndpoint, ArrayNode flowsArray, String condition) throws XPathExpressionException {
        // Extract flow name
        String targetEndpointName = xPath.evaluate("/TargetEndpoint/@name", targetEndpoint);

        // Create the flow object for the target endpoint
        ObjectNode flowObject = createFlowDefinition(targetEndpointName.concat("-flow"), condition, flowsArray);

        // Create request and response arrays in the flow object
        ArrayNode requestArray = flowObject.putArray(REQUEST);
        ArrayNode responseArray = flowObject.putArray(RESPONSE);

        // Add PreFlow Request steps to the request array and PostFlow Request steps to the response array
        processFlowNodes("/TargetEndpoint/PreFlow/Request/Step", targetEndpoint, apiGeePolicies, requestArray, "request");
        processFlowNodes("/TargetEndpoint/PostFlow/Request/Step", targetEndpoint, apiGeePolicies, requestArray, "request");

        // Add dynamic routing to override the url and send request to the backend service specified in the target endpoint
        buildRoutingPolicy(targetEndpoint, requestArray);

        // Add PreFlow Response steps to the response array and PostFlow Response steps to the response array
        processFlowNodes("/TargetEndpoint/PreFlow/Response/Step", targetEndpoint, apiGeePolicies, responseArray, "response");
        processFlowNodes("/TargetEndpoint/PostFlow/Response/Step", targetEndpoint, apiGeePolicies, responseArray, "response");
    }

    private void processFlowNodes(String xpathExpression, Document targetEndpoint, List<Document> apiGeePolicies, ArrayNode phaseArray, String phase) throws XPathExpressionException {
        NodeList flowNodes = (NodeList) xPath.evaluate(xpathExpression, targetEndpoint, XPathConstants.NODESET);
        applyPoliciesToFlowNodes(flowNodes, apiGeePolicies, phaseArray, phase);
    }

    public void applyPoliciesToFlowNodes(NodeList stepNodes, List<Document> apiGeePolicies, ArrayNode objectNodes, String scope) throws XPathExpressionException {
        if (stepNodes != null && stepNodes.getLength() > 0) {

            for (int j = 0; j < stepNodes.getLength(); j++) {
                var stepNode = stepNodes.item(j);
                findAndApplyMatchingPolicy(stepNode, apiGeePolicies, objectNodes, scope);
            }
        }
    }

    private void buildRoutingPolicy(Document targetEndpoint, ArrayNode requestArray) throws XPathExpressionException {
        // Add routing policy to the request array
        var routingPolicy = requestArray.addObject();

        routingPolicy.put(NAME, "Routing Policy");
        routingPolicy.put(ENABLED, true);
        routingPolicy.put(ASYNC, false);
        routingPolicy.put(POLICY, "dynamic-routing");

        var routingPolicyConf = routingPolicy.putObject(CONFIGURATION);
        var rulesArray = routingPolicyConf.putArray(RULES);
        var ruleObject = rulesArray.addObject();
        ruleObject.put(PATTERN, ".*");

        var targetEndpointUrl = xPath.evaluate("/TargetEndpoint/HTTPTargetConnection/URL", targetEndpoint);
        var targetUrl = constructEndpointsUrl(targetEndpointUrl);
        ruleObject.put(URL, targetUrl);
    }

    private ObjectNode buildPlanNode(String planName, ArrayNode plansArray) {
        // Create the plan node
        var plansNode = plansArray.addObject();

        // Hardcoded values
        plansNode.put(DEFINITION_VERSION, V4);
        plansNode.put(ID, UUID.randomUUID().toString());
        plansNode.put(DESCRIPTION, planName.concat("-plan"));
        plansNode.put(STATUS, "PUBLISHED");
        plansNode.put(VALIDATION, "MANUAL");

        // Name extracted from the API name
        plansNode.put(NAME, planName);

        // Build the security node inside the plan object
        buildPlansSecurity(plansNode);

        return plansNode;
    }

    private void buildPlansSecurity(ObjectNode plansNode) {
        // Hardcoded values
        var securityNode = plansNode.putObject(SECURITY);
        securityNode.put(TYPE, KEY_LESS);
        securityNode.putObject(CONFIGURATION);
    }
}