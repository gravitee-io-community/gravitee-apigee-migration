package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.CONFIGURATION;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.NAME;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.ASSIGN_MESSAGE;

/**
 * Converts AssignMessage policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the AssignMessage policy.
 */
@Component
@RequiredArgsConstructor
public class AssignMessageConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return ASSIGN_MESSAGE.equals(policyType);
    }

    /**
     * Constructs the AssignMessage policy.
     *
     * @param stepNode     The request step node to which the policy will be added.
     * @param apiGeePolicy The API Gee policy document.
     * @throws XPathExpressionException If an error occurs while evaluating the XPath expression.
     */
    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws XPathExpressionException {
        // Extracts the <AssignMessage> tag from the API Gee policy
        var name = xPath.evaluate("/AssignMessage/@name", apiGeePolicy);
        // Extracts the <AssignMessage> tag from the API Gee policy
        var scope = xPath.evaluate("/AssignMessage/AssignTo/@type", apiGeePolicy);

        constructAssignMessagePolicies(name, stepNode, apiGeePolicy, scopeArray, scope);
    }

    /**
     * Constructs the AssignMessage policies.
     * , should e split into multiple Gravitee policies depending on the present tags.
     */
    private void constructAssignMessagePolicies(String name, Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws XPathExpressionException {
        addAssignVariablePolicy(name, stepNode, apiGeePolicy, scopeArray);
        addSetHeadersPolicy(name, stepNode, apiGeePolicy, scopeArray);
        addSetVerbPolicy(name, stepNode, apiGeePolicy, scopeArray);
        addSetPayloadPolicy(name, stepNode, apiGeePolicy, scopeArray, scope);
    }

    private void addAssignVariablePolicy(String name, Node stepNode, Document apiGeePolicy, ArrayNode scopeArray) throws XPathExpressionException {
        // Extracts the <AssignVariable> nodes from the <AssignMessage> tag
        var assignVariableNodes = (NodeList) xPath.evaluate("//AssignVariable", apiGeePolicy, XPathConstants.NODESET);

        // Check if there are any <AssignVariable> nodes
        if (assignVariableNodes.getLength() != 0) {
            var requestNode = createBaseScopeNode(stepNode, name, "policy-assign-attributes", scopeArray);
            var configurationObject = requestNode.putObject(CONFIGURATION);
            var scope = xPath.evaluate("/AssignMessage/AssignTo/@type", apiGeePolicy);
            configurationObject.put("scope", scope.toUpperCase());

            var attributesArray = configurationObject.putArray("attributes");

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

    /**
     * Creates and adds value to the attribute node.
     * Depending on the tag name (<Value>, <Template>, <Ref>), it sets the value of the attribute node.
     *
     * @param assignVariableNode The <AssignVariable> node.
     * @param attributeNode      The attribute node to which the value will be added.
     */
    private void addValue(Node assignVariableNode, ObjectNode attributeNode) throws XPathExpressionException {
        var value = xPath.evaluate("Value", assignVariableNode);
        var template = xPath.evaluate("Template", assignVariableNode);
        var ref = xPath.evaluate("Ref", assignVariableNode);

        if (value != null && !value.isEmpty()) {
            attributeNode.put("value", value);
        } else if (template != null && !template.isEmpty()) {
            attributeNode.put("value", enhancedConvertExpression(template));
        } else if (ref != null && !ref.isEmpty()) {
            attributeNode.put("value", ref);
        } else {
            attributeNode.put("value", "");
        }
    }

    private void addSetHeadersPolicy(String name, Node requestStepNode, Document apiGeePolicy, ArrayNode scopeArray)
            throws XPathExpressionException {

        var setHeadersHeader = (NodeList) xPath.evaluate("//Set/Headers/Header", apiGeePolicy, XPathConstants.NODESET);
        if (setHeadersHeader.getLength() > 0) {
            var headersRequestNode = createBaseScopeNode(requestStepNode, name.concat("-Set-Headers"), "transform-headers", scopeArray);
            var configObject = headersRequestNode.putObject("configuration");
            var addedHeadersArray = configObject.putArray("addHeaders");

            configureAddedHeaders(setHeadersHeader, addedHeadersArray);
            configureRemovedHeaders(apiGeePolicy, configObject);
            configurePayloadContentType(apiGeePolicy, addedHeadersArray);
        }
    }

    private void configureAddedHeaders(NodeList setHeaders, ArrayNode headersArray) throws XPathExpressionException {
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

            headerObject.put("name", headerName);
            headerObject.put("value", headerValue);
        }
    }

    private String replaceCurlyBraceAttributes(String value) {
        if (value.contains("{") && value.contains("}")) {
            String attributeKey = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
            return value.replace("{" + attributeKey + "}", "{#context.attributes['" + attributeKey + "']}");
        }
        return value;
    }

    private void configureRemovedHeaders(Document apiGeePolicy, ObjectNode configObject) throws XPathExpressionException {
        var removeHeaders = (NodeList) xPath.evaluate("//Remove/Headers/Header", apiGeePolicy, XPathConstants.NODESET);
        if (removeHeaders.getLength() > 0) {
            var removeArray = configObject.putArray("removeHeaders");
            for (int i = 0; i < removeHeaders.getLength(); i++) {
                var headerNode = removeHeaders.item(i);
                var headerName = xPath.evaluate("@name", headerNode);
                removeArray.add(headerName);
            }
        }
    }

    private void configurePayloadContentType(Document apiGeePolicy, ArrayNode headersArray) throws XPathExpressionException {
        var payloadNodes = (NodeList) xPath.evaluate("//Set/Payload", apiGeePolicy, XPathConstants.NODESET);
        if (payloadNodes.getLength() > 0) {
            var payloadNode = payloadNodes.item(0);
            var contentType = xPath.evaluate("@contentType", payloadNode);
            if (contentType != null && !contentType.isEmpty()) {
                ObjectNode headerObject = headersArray.addObject();
                headerObject.put("name", "Content-Type");
                headerObject.put("value", contentType);
            }
        }
    }

    private void addSetVerbPolicy(String name, Node requestStepNode, Document apiGeePolicy, ArrayNode scopeArray) throws XPathExpressionException {
        var verbPolicy = (Node) xPath.evaluate("//Set/Verb", apiGeePolicy, XPathConstants.NODE);

        if (verbPolicy != null) {
            var verbRequestNode = createBaseScopeNode(requestStepNode, name.concat("-Set Verb"), "policy-override-request-method", scopeArray);
            var configurationObject = verbRequestNode.putObject("configuration");

            var verb = verbPolicy.getTextContent();
            configurationObject.put("method", verb);
        }
    }

    private void addSetPayloadPolicy(String name, Node requestStepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws XPathExpressionException {
        var payloadNode = (Node) xPath.evaluate("//Set/Payload", apiGeePolicy, XPathConstants.NODE);

        if (payloadNode != null) {
            var payloadRequestNode = createBaseScopeNode(requestStepNode, name.concat("-Set-Payload"), "policy-assign-content", scopeArray);
            var configurationObject = payloadRequestNode.putObject("configuration");
            configurationObject.put("scope", scope.toUpperCase());

            var body = payloadNode.getTextContent().trim().replaceAll("\\s+", "").replace("{", "").replace("}", "");
            configurationObject.put("body", "{#attributes['" + body + "']}");
        }
    }

    // TODO: ADD CASES WHEN WE HAVE REPLACE ALL, SUBSTRING AND ESCAPEJSON, AND JSONPATH
    private static String convertFunctionCall(String funcName, String args) {
        var parts = args.split(",");
        var convertedParts = processFunctionArguments(parts);
        return applyFunctionOrFallback(funcName, convertedParts);
    }

    private static List<String> processFunctionArguments(String[] parts) {
        List<String> convertedParts = new ArrayList<>();
        for (var part : parts) {
            var trimmedPart = part.trim();
            if (trimmedPart.startsWith("'") && trimmedPart.endsWith("'")) {
                convertedParts.add(trimmedPart);
            } else if (trimmedPart.startsWith("request.header.")) {
                String headerName = trimmedPart.substring("request.header.".length());
                convertedParts.add("#request.headers['" + headerName + "']");
            } else {
                convertedParts.add("#context.getVariable('" + trimmedPart + "')");
            }
        }
        return convertedParts;
    }

    private static String applyFunctionOrFallback(String funcName, List<String> convertedParts) {
        if ("firstnonnull".equalsIgnoreCase(funcName)) {
            return "{" + String.join(" ?: ", convertedParts) + "}";
        }
        return "{#" + funcName + "(" + String.join(", ", convertedParts) + ")}";
    }

    private static String convertBracedExpression(String input) {
        String inner = extractBracedInner(input);
        if (isFunctionCall(inner)) {
            return transformFunctionCall(inner);
        }
        return "{#context.getVariable('" + inner + "')}";
    }

    private static String extractBracedInner(String input) {
        return input.substring(1, input.length() - 1).trim();
    }

    private static boolean isFunctionCall(String inner) {
        return inner.contains("(") && inner.endsWith(")");
    }

    private static String transformFunctionCall(String inner) {
        int idx = inner.indexOf('(');
        String funcName = inner.substring(0, idx).trim();
        String args = inner.substring(idx + 1, inner.length() - 1).trim();
        return convertFunctionCall(funcName, args);
    }

    public static String enhancedConvertExpression(String input) {
        if (input == null) {
            return null;
        }
        List<String> tokens = splitExpressionIntoTokens(input);
        return buildConvertedString(tokens);
    }

    private static List<String> splitExpressionIntoTokens(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inBraces = false;
        int braceDepth = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (isOpeningBrace(c)) {
                handleOpeningBrace(tokens, token, inBraces, braceDepth);
                inBraces = true;
                braceDepth++;
                token.append(c);
            } else if (isClosingBrace(c)) {
                handleClosingBrace(tokens, token, braceDepth);
                braceDepth--;
                inBraces = braceDepth > 0;
            } else if (isDot(c) && !inBraces) {
                handleDot(tokens, token);
            } else {
                token.append(c);
            }
        }

        if (!token.isEmpty()) {
            tokens.add(token.toString());
        }

        return tokens;
    }

    private static boolean isOpeningBrace(char c) {
        return c == '{';
    }

    private static boolean isClosingBrace(char c) {
        return c == '}';
    }

    private static boolean isDot(char c) {
        return c == '.';
    }

    private static void handleOpeningBrace(List<String> tokens, StringBuilder token, boolean inBraces, int braceDepth) {
        if (braceDepth == 0 && !token.isEmpty()) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }

    private static void handleClosingBrace(List<String> tokens, StringBuilder token, int braceDepth) {
        token.append('}');
        if (braceDepth == 1) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }

    private static void handleDot(List<String> tokens, StringBuilder token) {
        if (!token.isEmpty()) {
            tokens.add(token.toString());
            token.setLength(0);
        }
        tokens.add("."); // keep the dot
    }

    private static String buildConvertedString(List<String> tokens) {
        StringBuilder result = new StringBuilder();
        for (String t : tokens) {
            if (t.equals(".")) {
                result.append(".");
            } else if (t.startsWith("{") && t.endsWith("}")) {
                result.append(convertBracedExpression(t));
            } else if (!t.isEmpty()) {
                result.append("{#context.getVariable('").append(t).append("')}");
            }
        }
        return result.toString();
    }
}