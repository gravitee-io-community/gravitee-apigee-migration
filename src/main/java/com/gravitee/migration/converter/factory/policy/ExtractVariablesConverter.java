package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.EXTRACT_VARIABLES;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.ASSIGN_ATTRIBUTES;

/**
 * Converts ExtractVariables policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the ExtractVariables policy.
 */
@Component
@RequiredArgsConstructor
public class ExtractVariablesConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return EXTRACT_VARIABLES.equals(policyType);
    }

    /**
     * Extracts variables from the specified source and stores them in the context attributes.
     *
     * @param stepNode      The XML node representing the ExtractVariables policy.
     * @param apiGeePolicy  The APIgee policy document.
     * @param requestArray  The array node to which the converted policy will be added.
     * @param phase         The phase of the policy (e.g., request, response).
     * @throws XPathExpressionException if an error occurs during XPath evaluation.
     */
    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode requestArray, String phase) throws XPathExpressionException {
        var policyName = xPath.evaluate("/ExtractVariables/@name", apiGeePolicy);
        var prefix = xPath.evaluate("/ExtractVariables/VariablePrefix", apiGeePolicy);
        var source = xPath.evaluate("/ExtractVariables/Source", apiGeePolicy);

        if (hasJsonPayloadVariables(apiGeePolicy)) {
            processJsonPayloadVariables(stepNode, apiGeePolicy, requestArray, policyName, prefix, source);
        } else if (hasXmlPayloadVariables(apiGeePolicy)) {
            processXmlPayloadVariables(stepNode, apiGeePolicy, requestArray, policyName, prefix, source);
        }
    }

    private boolean hasJsonPayloadVariables(Document apiGeePolicy) throws XPathExpressionException {
        var jsonPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/JSONPayload/Variable", apiGeePolicy, XPathConstants.NODESET);
        return jsonPayloadVariables != null && jsonPayloadVariables.getLength() > 0;
    }

    private boolean hasXmlPayloadVariables(Document apiGeePolicy) throws XPathExpressionException {
        var xmlPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/XMLPayload/Variable", apiGeePolicy, XPathConstants.NODESET);
        return xmlPayloadVariables != null && xmlPayloadVariables.getLength() > 0;
    }

    private void processJsonPayloadVariables(Node stepNode, Document apiGeePolicy, ArrayNode requestArray, String policyName, String prefix, String source) throws XPathExpressionException {
        var jsonPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/JSONPayload/Variable", apiGeePolicy, XPathConstants.NODESET);
        var scopeObject = createBaseScopeNode(stepNode, policyName, ASSIGN_ATTRIBUTES, requestArray);
        var attributes = scopeObject.putObject(CONFIGURATION).putArray(ATTRIBUTES);

        for (int i = 0; i < jsonPayloadVariables.getLength(); i++) {
            var jsonPayloadVariable = jsonPayloadVariables.item(i);
            addJsonPayloadAttribute(attributes, jsonPayloadVariable, prefix, source);
        }
    }

    private void addJsonPayloadAttribute(ArrayNode attributes, Node jsonPayloadVariable, String prefix, String source) throws XPathExpressionException {
        var attributeNode = attributes.addObject();
        var jsonVariableName = xPath.evaluate("@name", jsonPayloadVariable);
        var jsonPath = xPath.evaluate("JSONPath", jsonPayloadVariable);

        String contentSource = determineContentSource(source);

        attributeNode.put(NAME, buildAttributeName(prefix, jsonVariableName));
        attributeNode.put(VALUE, "{#jsonPath(" + contentSource + ", '" + jsonPath + "')}");
    }

    private void processXmlPayloadVariables(Node stepNode, Document apiGeePolicy, ArrayNode requestArray, String policyName, String prefix, String source) throws XPathExpressionException {
        var xmlPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/XMLPayload/Variable", apiGeePolicy, XPathConstants.NODESET);
        var scopeObject = createBaseScopeNode(stepNode, policyName, ASSIGN_ATTRIBUTES, requestArray);
        var attributes = scopeObject.putObject(CONFIGURATION).putArray(ATTRIBUTES);

        for (int i = 0; i < xmlPayloadVariables.getLength(); i++) {
            var xmlPayloadVariable = xmlPayloadVariables.item(i);
            addXmlPayloadAttribute(attributes, xmlPayloadVariable, prefix, source);
        }
    }

    private void addXmlPayloadAttribute(ArrayNode attributes, Node xmlPayloadVariable, String prefix, String source) throws XPathExpressionException {
        var attributeNode = attributes.addObject();
        var xmlVariableName = xPath.evaluate("@name", xmlPayloadVariable);
        var xPathContent = xPath.evaluate("XPath", xmlPayloadVariable);

        String contentSource = determineContentSource(source);

        attributeNode.put(NAME, buildAttributeName(prefix, xmlVariableName));
        attributeNode.put(VALUE, "{#xmlPath(" + contentSource + ", '" + xPathContent + "')}");
    }

    private String determineContentSource(String source) {
        if ("request".equalsIgnoreCase(source)) {
            return "#request.content";
        } else if ("response".equalsIgnoreCase(source)) {
            return "#response.content";
        } else {
            return "#context.attributes['" + source + "']";
        }
    }

    private String buildAttributeName(String prefix, String variableName) {
        return (prefix == null || prefix.isEmpty()) ? variableName : prefix + "." + variableName;
    }
}
