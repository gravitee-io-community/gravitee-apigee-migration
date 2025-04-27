package com.gravitee.migration.converter.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.enums.SignatureEnum;
import com.gravitee.migration.infrastructure.configuration.GraviteeELTranslator;
import com.gravitee.migration.service.filewriter.FileWriterService;
import com.gravitee.migration.util.policy.PolicyMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.gravitee.migration.util.GraviteeCliUtils.constructCondition;
import static com.gravitee.migration.util.StringUtils.*;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.*;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.*;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.DYNAMIC_ROUTING;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.JWT;

/**
 * This class is responsible for creating the plan array in the Gravitee JSON file
 * and mapping the flows from the Apigee proxy XML to the Gravitee format.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanObjectConverter {

    @Value("${gravitee.dictionary.name}")
    private String dictionaryName;
    private static final String ROUTING_POLICY_PATTERN = ".*";

    private final XPath xPath;
    private final FileWriterService fileWriterService;
    private final GraviteeELTranslator graviteeELTranslator;
    private final PolicyMapperUtil policyMapperUtil;
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
    public void createPlan(ObjectNode graviteeNode, String planName, List<Document> apiGeePolicies, List<Document> targetEndpoints, Document proxyXml, String apiProxyFolderLocation) throws XPathExpressionException, JsonProcessingException {

        var plansArray = graviteeNode.putArray(PLANS);
        // We are creating plans based on what type of policies are used in the API (JWT, API Key, etc.)
        var planNode = buildPlanNode(planName, plansArray);

        // Create the flows array inside the plan node
        var flowsArray = planNode.putArray(FLOWS);

        // Map flows from the proxy XML located in the proxies folder
        buildFlowsFromProxyXml(apiGeePolicies, proxyXml, targetEndpoints, flowsArray, apiProxyFolderLocation);
        buildPlansSecurity(planNode, plansArray, planName);
        processCachePolicies();
    }

    private void buildFlowsFromProxyXml(List<Document> apiGeePolicies, Document proxyXml, List<Document> targetEndpoints, ArrayNode flowsArray, String apiProxyFolderLocation) throws XPathExpressionException {
        // Add PreFlow Request steps to the request array
        buildPreOrPostFlowSteps("/*/PreFlow/Request/Step", REQUEST, "PreFlow-Request", apiGeePolicies, proxyXml, flowsArray, apiProxyFolderLocation);

        // Add Conditional flows to the flows array
        buildConditionalFlows(proxyXml, flowsArray, apiGeePolicies, targetEndpoints, apiProxyFolderLocation);

        // Add PreFlow Response steps to the response array
        buildPreOrPostFlowSteps("/*/PostFlow/Response/Step", RESPONSE, "PostFlow-Response", apiGeePolicies, proxyXml, flowsArray, apiProxyFolderLocation);
    }

    private void buildPreOrPostFlowSteps(String xpathExpression, String phase, String name, List<Document> apiGeePolicies, Document proxyXml, ArrayNode flowsArray, String apiProxyFolderLocation) throws XPathExpressionException {
        var flowNodes = (NodeList) xPath.evaluate(xpathExpression, proxyXml, XPathConstants.NODESET);
        // PreFlow and PostFlow have no conditions - they are always executed
        buildProxyFlow(flowNodes, flowsArray, phase, name, apiGeePolicies, null, apiProxyFolderLocation);
    }

    private void buildConditionalFlows(Document proxyXml, ArrayNode flowsArray, List<Document> apiGeePolicies, List<Document> targetEndpoints, String apiProxyFolderLocation) throws XPathExpressionException {
        // Extract all conditional flows from the proxyXml
        var conditionalFlows = (NodeList) xPath.evaluate("/*/Flows/Flow", proxyXml, XPathConstants.NODESET);

        // For each flow add the request steps to the flows array
        processConditionalFlowSteps(conditionalFlows, "Request/Step", REQUEST, "-Request", apiGeePolicies, flowsArray, apiProxyFolderLocation);

        // Add PostFlow Request steps to the request array
        buildPreOrPostFlowSteps("/*/PostFlow/Request/Step", REQUEST, "PostFlow-Request", apiGeePolicies, proxyXml, flowsArray, apiProxyFolderLocation);

        // Build routing rules
        buildRouteRules(apiGeePolicies, proxyXml, targetEndpoints, flowsArray, apiProxyFolderLocation);

        // Add PreFlow Response steps to the response array
        buildPreOrPostFlowSteps("/*/PreFlow/Response/Step", RESPONSE, "PreFlow-Response", apiGeePolicies, proxyXml, flowsArray, apiProxyFolderLocation);

        // For each flow add the response steps to the flows array
        processConditionalFlowSteps(conditionalFlows, "Response/Step", RESPONSE, "-Response", apiGeePolicies, flowsArray, apiProxyFolderLocation);
    }

    private void processConditionalFlowSteps(NodeList conditionalFlows, String stepXPath, String phase, String suffix, List<Document> apiGeePolicies, ArrayNode flowsArray, String apiProxyFolderLocation) throws XPathExpressionException {
        for (int i = 0; i < conditionalFlows.getLength(); i++) {
            var conditionalFlow = conditionalFlows.item(i);
            var conditionalFlowName = xPath.evaluate("@name", conditionalFlow);
            var conditionalFlowCondition = xPath.evaluate("Condition", conditionalFlow);

            var flowNodes = (NodeList) xPath.evaluate(stepXPath, conditionalFlow, XPathConstants.NODESET);
            buildProxyFlow(flowNodes, flowsArray, phase, conditionalFlowName.concat(suffix), apiGeePolicies, conditionalFlowCondition, apiProxyFolderLocation);
        }
    }

    private void buildProxyFlow(NodeList steps, ArrayNode flowsArray, String phase, String name, List<Document> apiGeePolicies, String condition, String apiProxyFolderLocation) throws XPathExpressionException {
        if (steps != null && steps.getLength() > 0) {

            // Create the flow object
            var flowObject = createFlowDefinition(name, condition, flowsArray);
            // Create the request or response array inside the flow object
            var scopeArray = flowObject.putArray(phase);

            // Process the steps inside the flow and map them to the corresponding policies
            for (int i = 0; i < steps.getLength(); i++) {
                var stepNode = steps.item(i);
                policyMapperUtil.findAndApplyMatchingPolicy(stepNode, apiGeePolicies, scopeArray, phase, false, apiProxyFolderLocation);
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
        constructCondition(flowCondition, selectorsObject, graviteeELTranslator.getConditionMappings(), selectors);

        return flowNode;
    }

    private void buildRouteRules(List<Document> apiGeePolicies, Document proxyXml, List<Document> targetEndpoints, ArrayNode flowsArray, String apiProxyFolderLocation) throws XPathExpressionException {
        // Extract all route rules from the proxyXml
        NodeList routeRules = (NodeList) xPath.evaluate("/ProxyEndpoint/RouteRule", proxyXml, XPathConstants.NODESET);

        // Process each route rule
        for (int i = 0; i < routeRules.getLength(); i++) {
            Node routeRule = routeRules.item(i);
            processRouteRule(routeRule, apiGeePolicies, targetEndpoints, flowsArray, apiProxyFolderLocation);
        }
    }

    private void processRouteRule(Node routeRule, List<Document> apiGeePolicies, List<Document> targetEndpoints, ArrayNode flowsArray, String apiProxyFolderLocation) throws XPathExpressionException {
        // Extract the target endpoint name and condition
        String routeRuleTargetEndpoint = xPath.evaluate("TargetEndpoint", routeRule);
        String condition = xPath.evaluate("Condition", routeRule);

        // Find the matching target endpoint
        Document matchingTargetEndpoint = findMatchingTargetEndpoint(routeRuleTargetEndpoint, targetEndpoints);
        if (matchingTargetEndpoint != null) {
            // Build the flows for the matching target endpoint
            buildTargetEndpointFlows(apiGeePolicies, matchingTargetEndpoint, flowsArray, condition, apiProxyFolderLocation);
        }
    }

    private Document findMatchingTargetEndpoint(String routeRuleTargetEndpoint, List<Document> targetEndpoints) throws XPathExpressionException {
        // Iterate through the target endpoints to find a match
        for (Document targetEndpoint : targetEndpoints) {
            // Extract target endpoint name
            String targetEndpointName = xPath.evaluate("TargetEndpoint/@name", targetEndpoint);
            if (routeRuleTargetEndpoint.equals(targetEndpointName)) {
                var targetEndpointUrl = xPath.evaluate("TargetEndpoint/HTTPTargetConnection/URL", targetEndpoint);

                if (isNotNullOrEmpty(targetEndpointUrl)) {
                    var baseUrl = extractBaseUrl(targetEndpointUrl);

                    var cleanedBaseUrl = removeCurlyBraces(baseUrl);

                    fileWriterService.addValueToDictionaryMap(cleanedBaseUrl, CHANGE_ME);
                }
                return targetEndpoint;
            }
        }
        return null;
    }

    private void buildTargetEndpointFlows(List<Document> apiGeePolicies, Document targetEndpoint, ArrayNode flowsArray, String condition, String apiProxyFolderLocation) throws XPathExpressionException {
        // Extract flow name
        String targetEndpointName = xPath.evaluate("/TargetEndpoint/@name", targetEndpoint);

        // Create the flow object for the target endpoint
        ObjectNode flowObject = createFlowDefinition(targetEndpointName.concat("-flow"), condition, flowsArray);

        // Create request and response arrays in the flow object
        ArrayNode requestArray = flowObject.putArray(REQUEST);
        ArrayNode responseArray = flowObject.putArray(RESPONSE);

        // Add PreFlow Request steps to the request array and PostFlow Request steps to the response array
        processFlowNodes("/TargetEndpoint/PreFlow/Request/Step", targetEndpoint, apiGeePolicies, requestArray, REQUEST, apiProxyFolderLocation);
        processFlowNodes("/TargetEndpoint/PostFlow/Request/Step", targetEndpoint, apiGeePolicies, requestArray, REQUEST, apiProxyFolderLocation);

        // Add dynamic routing to override the url and send request to the backend service specified in the target endpoint
        buildRoutingPolicy(targetEndpoint, requestArray);

        // Add PreFlow Response steps to the response array and PostFlow Response steps to the response array
        processFlowNodes("/TargetEndpoint/PreFlow/Response/Step", targetEndpoint, apiGeePolicies, responseArray, RESPONSE, apiProxyFolderLocation);
        processFlowNodes("/TargetEndpoint/PostFlow/Response/Step", targetEndpoint, apiGeePolicies, responseArray, RESPONSE, apiProxyFolderLocation);
    }

    private void processFlowNodes(String xpathExpression, Document targetEndpoint, List<Document> apiGeePolicies, ArrayNode phaseArray, String phase, String apiGeeFolderLocation) throws XPathExpressionException {
        NodeList flowNodes = (NodeList) xPath.evaluate(xpathExpression, targetEndpoint, XPathConstants.NODESET);
        policyMapperUtil.applyPoliciesToFlowNodes(flowNodes, apiGeePolicies, phaseArray, phase, false, apiGeeFolderLocation);
    }

    private void buildRoutingPolicy(Document targetEndpoint, ArrayNode requestArray) throws XPathExpressionException {
        // Add routing policy to the request array
        var routingPolicy = requestArray.addObject();

        routingPolicy.put(NAME, ROUTING_POLICY);
        routingPolicy.put(ENABLED, true);
        routingPolicy.put(ASYNC, false);
        routingPolicy.put(POLICY, DYNAMIC_ROUTING);

        var routingPolicyConf = routingPolicy.putObject(CONFIGURATION);
        var rulesArray = routingPolicyConf.putArray(RULES);
        var ruleObject = rulesArray.addObject();
        ruleObject.put(PATTERN, ROUTING_POLICY_PATTERN);

        setRoutingPolicyUrl(ruleObject, targetEndpoint);
    }

    private void setRoutingPolicyUrl(ObjectNode ruleObject, Document targetEndpoint) throws XPathExpressionException {
        var targetEndpointUrl = xPath.evaluate("/TargetEndpoint/HTTPTargetConnection/URL", targetEndpoint);

        if (isNotNullOrEmpty(targetEndpointUrl)) {
            var targetUrl = constructEndpointsUrl(targetEndpointUrl, dictionaryName);
            ruleObject.put(URL, targetUrl);
        } else {
            var loadBalancerServerUrl = xPath.evaluate("/TargetEndpoint/HTTPTargetConnection/LoadBalancer/Server/@name", targetEndpoint);
            ruleObject.put(URL, "{#endpoints['" + loadBalancerServerUrl + "']}");
        }
    }

    private ObjectNode buildPlanNode(String planName, ArrayNode plansArray) {
        // Create the plan node
        var plansNode = plansArray.addObject();

        // Hardcoded values
        plansNode.put(DEFINITION_VERSION, V4);
        plansNode.put(ID, UUID.randomUUID().toString());
        plansNode.put(DESCRIPTION, planName.concat("-plan"));
        plansNode.put(STATUS, STAGING);
        plansNode.put(VALIDATION, MANUAL);

        // Name extracted from the API name
        plansNode.put(NAME, planName);

        return plansNode;
    }

    private void buildPlansSecurity(ObjectNode plansNode, ArrayNode plansArray, String planName) throws JsonProcessingException {
        Set<String> collectedPolicies = policyMapperUtil.getCollectedPolicies();

        if (collectedPolicies.isEmpty()) {
            buildKeylessPlan(plansNode);
        } else if (collectedPolicies.contains(VERIFY_JWT) && collectedPolicies.contains(VERIFY_API_KEY)) {
            buildApiKeyPlan(plansNode, planName);
            buildNewJWTPlan(plansNode, plansArray, planName);
        } else if (collectedPolicies.contains(VERIFY_JWT)) {
            buildJwtPlan(plansNode, planName);
        } else if (collectedPolicies.contains(VERIFY_API_KEY)) {
            buildApiKeyPlan(plansNode, planName);
        }
    }

    private void buildApiKeyPlan(ObjectNode plansObject, String planName) {
        plansObject.put(NAME, planName.concat("_").concat(API_KEY));
        var securityNode = plansObject.putObject(SECURITY);
        securityNode.put(TYPE, API_KEY);
        securityNode.putObject(CONFIGURATION);
    }

    private void buildKeylessPlan(ObjectNode plansObject) {
        var securityNode = plansObject.putObject(SECURITY);
        securityNode.put(TYPE, KEY_LESS);
        securityNode.putObject(CONFIGURATION);
    }

    private void buildNewJWTPlan(ObjectNode plansNode, ArrayNode plansArray, String planName) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        var jwtPlan = objectMapper.readValue(plansNode.toString(), ObjectNode.class);
        jwtPlan.put(ID, UUID.randomUUID().toString());
        jwtPlan.put(NAME, planName.concat("_").concat(JWT.toUpperCase()));

        plansArray.add(jwtPlan);

        var securityNode = jwtPlan.putObject(SECURITY);
        securityNode.put(TYPE, JWT.toUpperCase());

        configureJwtPlan(securityNode.putObject(CONFIGURATION));
    }

    private void buildJwtPlan(ObjectNode plansObject, String planName) {
        plansObject.put(NAME, planName.concat("_").concat(JWT.toUpperCase()));
        var securityNode = plansObject.putObject(SECURITY);
        securityNode.put(TYPE, JWT.toUpperCase());

        configureJwtPlan(securityNode.putObject(CONFIGURATION));
    }

    private void configureJwtPlan(ObjectNode configurationObject) {
        configureBasicJwtSettings(configurationObject);
        configureConfirmationMethodValidation(configurationObject.putObject(CONFIRMATION_METHOD_VALIDATION));
        configureTokenTypeValidation(configurationObject.putObject(TOKEN_TYP_VALIDATION));
    }

    private void configureBasicJwtSettings(ObjectNode configurationObject) {
        configurationObject.put(SIGNATURE, SignatureEnum.RS256.getValue());
        configurationObject.put(PUBLIC_KEY_RESOLVER, GIVEN_KEY);
        configurationObject.put(CONNECT_TIMEOUT, 2000);
        configurationObject.put(REQUEST_TIMEOUT, 2000);
        configurationObject.put(FOLLOW_REDIRECTS, false);
        configurationObject.put(USE_SYSTEM_PROXY, false);
        configurationObject.put(EXTRACT_CLAIMS, false);
        configurationObject.put(PROPAGATE_AUTH_HEADER, true);
        configurationObject.put(USER_CLAIM, SUB);
    }

    private void configureConfirmationMethodValidation(ObjectNode confirmationMethodValidation) {
        confirmationMethodValidation.put(IGNORE_MISSING, false);

        var certificateBoundThumbprint = confirmationMethodValidation.putObject(CERTIFICATE_BOUND_THUMBPRINT);
        certificateBoundThumbprint.put(ENABLED, false);
        certificateBoundThumbprint.put(EXTRACT_CERTIFICATE_FROM_HEADER, false);
        certificateBoundThumbprint.put(HEADER_NAME, SSL_CLIENT_CERT);
    }

    private void configureTokenTypeValidation(ObjectNode tokenTypValidation) {
        tokenTypValidation.put(ENABLED, false);
        tokenTypValidation.put(IGNORE_MISSING, false);

        var expectedValues = tokenTypValidation.putArray(EXPECTED_VALUES);
        expectedValues.add(JWT.toUpperCase());

        tokenTypValidation.put(IGNORE_CASE, false);
    }

    private void processCachePolicies() throws XPathExpressionException {
        var documents = policyMapperUtil.getCachePolicies();

        for (var policy : documents) {
            apiObjectConverter.buildResources(policy);
        }
    }

}