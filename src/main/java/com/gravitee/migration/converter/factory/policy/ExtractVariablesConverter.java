package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.ACCESS_ENTITY;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.EXTRACT_VARIABLES;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.ASSIGN_ATTRIBUTES;

/**
 * <p>Converts ExtractVariables policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the ExtractVariables policy.</p>
 */
@Component
@RequiredArgsConstructor
public class ExtractVariablesConverter implements PolicyConverter {

    @Value("${gravitee.dictionary.name}")
    private String dictionaryName;

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return EXTRACT_VARIABLES.equals(policyType);
    }

    /**
     * Extracts variables from the specified source and stores them in the context attributes.
     *
     * @param condition    The condition to be applied to the policy.
     * @param apiGeePolicy The Apigee policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.q, request or response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws XPathExpressionException if an error occurs during XPath evaluation.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Extract properties
        var policyName = xPath.evaluate("/ExtractVariables/@name", apiGeePolicy);
        var prefix = xPath.evaluate("/ExtractVariables/VariablePrefix", apiGeePolicy);
        var source = xPath.evaluate("/ExtractVariables/Source", apiGeePolicy);

//        processURIPatter();
        // Check if we need to extract variables from JSON or XML payload
        if (hasJsonPayloadVariables(apiGeePolicy)) {
            processJsonPayloadVariables(condition, apiGeePolicy, phaseArray, policyName, prefix, source, phase, conditionMappings);
        } else if (hasXmlPayloadVariables(apiGeePolicy)) {
            processXmlPayloadVariables(condition, apiGeePolicy, phaseArray, policyName, prefix, source, phase, conditionMappings);
        }
    }

//    private void processURIPatter(Document apiGeePolicy){
//        var uriPaths = (NodeList) xPath.evaluate("/ExtractVariables/URIPath/Pattern", apiGeePolicy, XPathConstants.NODESET);
//    }

    private boolean hasJsonPayloadVariables(Document apiGeePolicy) throws XPathExpressionException {
        var jsonPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/JSONPayload/Variable", apiGeePolicy, XPathConstants.NODESET);
        return jsonPayloadVariables != null && jsonPayloadVariables.getLength() > 0;
    }

    private boolean hasXmlPayloadVariables(Document apiGeePolicy) throws XPathExpressionException {
        var xmlPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/XMLPayload/Variable", apiGeePolicy, XPathConstants.NODESET);
        return xmlPayloadVariables != null && xmlPayloadVariables.getLength() > 0;
    }

    private void processJsonPayloadVariables(String condition, Document apiGeePolicy, ArrayNode phaseArray, String policyName, String prefix, String source,
                                             String scope, Map<String, String> conditionMappings) throws XPathExpressionException {
        var jsonPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/JSONPayload/Variable", apiGeePolicy, XPathConstants.NODESET);

        var attributes = createScopeConfiguration(condition, policyName, scope, phaseArray, conditionMappings);

        for (int i = 0; i < jsonPayloadVariables.getLength(); i++) {
            var jsonPayloadVariable = jsonPayloadVariables.item(i);
            addJsonPayloadAttribute(attributes, jsonPayloadVariable, prefix, source);
        }
    }

    private void addJsonPayloadAttribute(ArrayNode attributes, Node jsonPayloadVariable, String prefix, String source) throws XPathExpressionException {
        var attributeNode = attributes.addObject();
        // Extract the JSON variable name and JSONPath
        var jsonVariableName = xPath.evaluate("@name", jsonPayloadVariable);
        var jsonPath = xPath.evaluate("JSONPath", jsonPayloadVariable);

        // Determine from what source we are extracting the content
        String contentSource = determineContentSource(source);

        // Add prefix to the variable name if it is not null or empty
        attributeNode.put(NAME, buildAttributeName(prefix, jsonVariableName));
        attributeNode.put(VALUE, String.format("{#jsonPath(%s, '%s')}", contentSource, jsonPath));
    }

    private void processXmlPayloadVariables(String condition, Document apiGeePolicy, ArrayNode phaseArray, String policyName, String prefix, String source,
                                            String scope, Map<String, String> conditionMappings) throws XPathExpressionException {
        var xmlPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/XMLPayload/Variable", apiGeePolicy, XPathConstants.NODESET);

        var attributes = createScopeConfiguration(condition, policyName, scope, phaseArray, conditionMappings);

        for (int i = 0; i < xmlPayloadVariables.getLength(); i++) {
            var xmlPayloadVariable = xmlPayloadVariables.item(i);
            addXmlPayloadAttribute(attributes, xmlPayloadVariable, prefix, source);
        }
    }

    private ArrayNode createScopeConfiguration(String condition, String policyName, String phase, ArrayNode phaseArray, Map<String, String> conditionMappings) throws XPathExpressionException {
        var scopeObject = createBasePhaseObject(condition, policyName, ASSIGN_ATTRIBUTES, phaseArray, conditionMappings);
        var configurationObject = scopeObject.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());

        return configurationObject.putArray(ATTRIBUTES);
    }

    private void addXmlPayloadAttribute(ArrayNode attributes, Node xmlPayloadVariable, String prefix, String source) throws XPathExpressionException {
        var attributeNode = attributes.addObject();
        // Extract the XML variable name and XPath
        var xmlVariableName = xPath.evaluate("@name", xmlPayloadVariable);
        var xPathContent = xPath.evaluate("XPath", xmlPayloadVariable);

        // Determine from what source we are extracting the content
        String contentSource = determineContentSource(source);

        // Add prefix to the variable name if it is not null or empty
        attributeNode.put(NAME, buildAttributeName(prefix, xmlVariableName));
        attributeNode.put(VALUE, String.format("{#xpath(%s, '%s')}", contentSource, xPathContent));
    }

    private String determineContentSource(String source) {
        if (source == null) {
            return null;
        }

        String normalizedSource = source.toLowerCase();

        switch (normalizedSource) {
            case REQUEST:
                return REQUEST_CONTENT;
            case RESPONSE:
                return RESPONSE_CONTENT;
            default:
                // If extracting from AccessEntity, we need to extract from the dictionary
                if (normalizedSource.startsWith(ACCESS_ENTITY.toLowerCase())) {
                    return String.format(DICTIONARY_FORMAT, dictionaryName, source);
                }
                // If the source ends with ".content", remove it, it is only used in Apigee, we are already defining .content in HTTP Callout policy
                if (source.endsWith(".content")) {
                    source = source.substring(0, source.lastIndexOf(".content"));
                }
                return String.format(CONTEXT_ATTRIBUTE_FORMAT, source);
        }
    }

    private String buildAttributeName(String prefix, String variableName) {
        return (prefix == null || prefix.isEmpty()) ? variableName : prefix + "." + variableName;
    }
}
