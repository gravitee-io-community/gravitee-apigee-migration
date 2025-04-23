package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.infrastructure.configuration.GraviteeELTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.convertApigeeConditionToGravitee;
import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.ADD_HEADERS;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.METHOD;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.ASSIGN_MESSAGE;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.*;

/**
 * <p>Converts the AssignMessage policy from Apigee to Gravitee format.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the AssignMessage policy.</p>
 */
@Component
@RequiredArgsConstructor
public class AssignMessageConverter implements PolicyConverter {

    private final XPath xPath;
    private final GraviteeELTranslator graviteeELTranslator;

    @Override
    public boolean supports(String policyType) {
        return ASSIGN_MESSAGE.equals(policyType);
    }

    /**
     * Converts the AssignMessage policy from Apigee to Gravitee.
     *
     * @param condition    The condition under which the policy is applied.
     * @param apiGeePolicy The Apigee policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws XPathExpressionException if an error occurs during XPath evaluation.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Extract properties
        var name = xPath.evaluate("/AssignMessage/@name", apiGeePolicy);

        constructAssignMessagePolicies(name, condition, apiGeePolicy, phaseArray, phase, conditionMappings);
    }

    /**
     * Constructs the AssignMessage policies.
     * The policy in Apigee is split into multiple Gravitee policies depending on the present tags.
     */
    private void constructAssignMessagePolicies(String name, String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        addAssignVariablePolicy(name, condition, apiGeePolicy, phaseArray, phase, conditionMappings);
        addSetHeadersPolicy(name, condition, apiGeePolicy, phaseArray, phase, conditionMappings);
        addSetVerbPolicy(name, condition, apiGeePolicy, phaseArray, phase, conditionMappings);
        addSetPayloadPolicy(name, condition, apiGeePolicy, phaseArray, phase, conditionMappings);
    }

    private void addAssignVariablePolicy(String name, String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Extracts the <AssignVariable> nodes from the <AssignMessage> tag
        var assignVariableNodes = (NodeList) xPath.evaluate("//AssignVariable", apiGeePolicy, XPathConstants.NODESET);

        // Check if there are any <AssignVariable> nodes
        if (assignVariableNodes.getLength() != 0) {
            var attributesArray = createAttributesInPhaseObject(condition, name, phase, phaseArray, conditionMappings);

            // For each <AssignVariable> node, create a new object in the attributes array
            for (int i = 0; i < assignVariableNodes.getLength(); i++) {
                var assignVariableNode = assignVariableNodes.item(i);
                var attributeNode = attributesArray.addObject();

                // Extract value from the <Name> tag inside <AssignVariable>
                var variableName = xPath.evaluate("Name", assignVariableNode);
                attributeNode.put(NAME, variableName);

                addValue(assignVariableNode, attributeNode);
            }
        }
    }

    private ArrayNode createAttributesInPhaseObject(String condition, String name, String phase, ArrayNode phaseArray, Map<String, String> conditionMappings) throws XPathExpressionException {
        var phaseObject = createBasePhaseObject(condition, name, ASSIGN_ATTRIBUTES, phaseArray, conditionMappings);
        var configurationObject = phaseObject.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());

        return configurationObject.putArray(ATTRIBUTES);
    }

    private void addValue(Node assignVariableNode, ObjectNode attributeNode) throws XPathExpressionException {
        // Value can be either a <Value> or a <Template> tag
        var value = xPath.evaluate("Value", assignVariableNode);
        var template = xPath.evaluate("Template", assignVariableNode);
        var ref = xPath.evaluate("Ref", assignVariableNode);

        if (isNotNullOrEmpty(value)) {
            attributeNode.put(VALUE, value);
        } else if (isNotNullOrEmpty(template)) {
            graviteeELTranslator.loadAssignMessageTemplateMappings();
            attributeNode.put(VALUE, convertApigeeConditionToGravitee(template, graviteeELTranslator.getAssignMessageTemplateMappings()));
        } else if (isNotNullOrEmpty(ref)) {
            attributeNode.put(VALUE, wrapValueInContextAttributes(ref));
        } else {
            attributeNode.put(VALUE, "");
        }
    }

    private void addSetHeadersPolicy(String name, String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        var setHeadersHeader = (NodeList) xPath.evaluate("//Set/Headers/Header", apiGeePolicy, XPathConstants.NODESET);

        if (setHeadersHeader != null && setHeadersHeader.getLength() > 0) {
            var headersRequestNode = createBasePhaseObject(condition, name.concat("-Transform-Headers"), TRANSFORM_HEADERS, phaseArray, conditionMappings);
            var configObject = headersRequestNode.putObject(CONFIGURATION);

            configObject.put(SCOPE, phase.toUpperCase());
            var addedHeadersArray = configObject.putArray(ADD_HEADERS);
            configureAddedHeaders(setHeadersHeader, addedHeadersArray);
            configurePayloadContentType(apiGeePolicy, addedHeadersArray);
        }

        configureRemovedHeaders(apiGeePolicy, name, phaseArray, condition, phase, conditionMappings);
    }

    private void configureAddedHeaders(NodeList setHeaders, ArrayNode headersArray) throws XPathExpressionException {
        // Translate headers to Gravitee EL
        for (int i = 0; i < setHeaders.getLength(); i++) {
            var headerNode = setHeaders.item(i);
            var headerObject = headersArray.addObject();

            var headerName = xPath.evaluate("@name", headerNode);
            var headerValue = headerNode.getTextContent();

            if (headerValue.startsWith("{request.header.")) {
                headerValue = headerValue
                        .replace("{request.header.", "{#request.headers['")
                        .replace("}", "']}");
            } else {
                headerValue = replaceCurlyBraceAttributes(headerValue);
            }

            headerObject.put(NAME, headerName);
            headerObject.put(VALUE, headerValue);
        }
    }

    private String replaceCurlyBraceAttributes(String value) {
        if (value.contains("{") && value.contains("}")) {
            String attributeKey = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
            return value.replace("{" + attributeKey + "}", wrapValueInContextAttributes(attributeKey));
        }
        return value;
    }

    private void configureRemovedHeaders(Document apiGeePolicy, String policyName, ArrayNode scopeArray, String condition, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        var removeHeaders = (NodeList) xPath.evaluate("/AssignMessage/Remove/Headers/Header", apiGeePolicy, XPathConstants.NODESET);

        if (removeHeaders != null && removeHeaders.getLength() > 0) {
            var headersRequestNode = createBasePhaseObject(condition, policyName.concat("-Transform-Headers"), TRANSFORM_HEADERS, scopeArray, conditionMappings);
            var configObject = headersRequestNode.putObject(CONFIGURATION);
            configObject.put(SCOPE, phase.toUpperCase());
            var removeArray = configObject.putArray(REMOVE_HEADERS);

            for (int i = 0; i < removeHeaders.getLength(); i++) {
                var headerNode = removeHeaders.item(i);
                var headerName = xPath.evaluate("@name", headerNode);
                removeArray.add(headerName);
            }
        }
    }

    private void configurePayloadContentType(Document apiGeePolicy, ArrayNode headersArray) throws XPathExpressionException {
        var payload = xPath.evaluate("/Set/Payload", apiGeePolicy);

        if (isNotNullOrEmpty(payload)) {
            var contentType = xPath.evaluate("/Set/Payload/@contentType", apiGeePolicy);

            if (contentType != null && !contentType.isEmpty()) {
                ObjectNode headerObject = headersArray.addObject();
                headerObject.put(NAME, CONTENT_TYPE);
                headerObject.put(VALUE, contentType);
            }
        }
    }

    private void addSetVerbPolicy(String name, String condition, Document apiGeePolicy, ArrayNode scopeArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        var verbPolicy = (Node) xPath.evaluate("//Set/Verb", apiGeePolicy, XPathConstants.NODE);

        if (verbPolicy != null) {
            var verbRequestNode = createBasePhaseObject(condition, name.concat("-Set Verb"), OVERRIDE_REQUEST_METHOD, scopeArray, conditionMappings);
            var configurationObject = verbRequestNode.putObject(CONFIGURATION);
            configurationObject.put(SCOPE, phase.toUpperCase());

            var verb = verbPolicy.getTextContent();
            configurationObject.put(METHOD, verb);
        }
    }

    private void addSetPayloadPolicy(String name, String condition, Document apiGeePolicy, ArrayNode scopeArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        var payload = xPath.evaluate("//Set/Payload/text()", apiGeePolicy);

        if (isNotNullOrEmpty(payload)) {
            var payloadRequestNode = createBasePhaseObject(condition, name.concat("-Set-Payload"), ASSIGN_CONTENT, scopeArray, conditionMappings);
            var configurationObject = payloadRequestNode.putObject(CONFIGURATION);
            configurationObject.put(SCOPE, phase.toUpperCase());

            var body = removeCurlyBraces(payload);
            configureBody(configurationObject, body);
        }
    }

    private void configureBody(ObjectNode configurationObject, String body) {
        if (REQUEST_CONTENT.equals(body)) {
            configurationObject.put(BODY, REQUEST_CONTENT_WRAPPED);
        } else if (RESPONSE_CONTENT.equals(body)) {
            configurationObject.put(BODY, RESPONSE_CONTENT_WRAPPED);
        } else {
            configurationObject.put(BODY, "${context.attributes['" + body + "']}");
        }
    }
}