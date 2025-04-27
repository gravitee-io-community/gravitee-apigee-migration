package com.gravitee.migration.converter.policy.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.policy.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.*;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.*;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.SERVICE_CALLOUT;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.HTTP_CALLOUT;

/**
 * <p>Converts ServiceCallout policy from Apigee to Gravitee.</p>
 *
 * <p>Implements the PolicyConverter interface and provides the logic
 * to convert the ServiceCallout policy.</p>
 */
@Component
@RequiredArgsConstructor
public class ServiceCalloutConverter implements PolicyConverter {

    @Value("${gravitee.dictionary.name}")
    private String dictionaryName;

    private final XPath xPath;
    private static final String PATH_DELIMITER = "/";

    @Override
    public boolean supports(String policyType) {
        return SERVICE_CALLOUT.equals(policyType);
    }

    /**
     * Converts the ServiceCallout policy from Apigee to Gravitee.
     *
     * @param condition  The condition to be applied to the policy.
     * @param apiGeePolicy The Apigee policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request or response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws XPathExpressionException if an error occurs during XPath evaluation.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Extract properties
        var policyName = xPath.evaluate("/ServiceCallout/@name", apiGeePolicy);
        var headersNodeList = (NodeList) xPath.evaluate("/ServiceCallout/Request/Set/Headers/Header", apiGeePolicy, XPathConstants.NODESET);
        var formParametersNodeList = (NodeList) xPath.evaluate("/ServiceCallout/Request/Set/FormParams/FormParam", apiGeePolicy, XPathConstants.NODESET);
        var method = xPath.evaluate("/ServiceCallout/Request/Set/Verb", apiGeePolicy);
        var path = xPath.evaluate("/ServiceCallout/Request/Set/Path", apiGeePolicy);
        var url = xPath.evaluate("/ServiceCallout/HTTPTargetConnection/URL", apiGeePolicy);
        var payload = xPath.evaluate("/ServiceCallout/Request/Set/Payload", apiGeePolicy);

        var configurationObjectNode = createConfigurationObject(condition, policyName, phaseArray, phase, conditionMappings);

        configureRequest(configurationObjectNode, method, url, path, formParametersNodeList, payload);
        processHeaders(headersNodeList, configurationObjectNode.putArray(HEADERS));
        configureResponseHandling(configurationObjectNode, apiGeePolicy);
    }

    private ObjectNode createConfigurationObject(String condition, String policyName, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        var objectNode = createBasePhaseObject(condition, policyName, HTTP_CALLOUT, phaseArray, conditionMappings);
        var configurationObject = objectNode.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());

        return configurationObject;
    }

    private void configureRequest(ObjectNode configurationObjectNode, String method, String url, String path, NodeList formParametersNodeList, String payload) throws XPathExpressionException {
        // Construct the full URL and append form parameters if present
        var fullUrl = validateAndBuildFullUrl(url, path, formParametersNodeList);

        // Set the HTTP method
        configurationObjectNode.put(METHOD, isNotNullOrEmpty(method) ? method : HttpMethod.GET.name());

        // Set the request body if a payload is provided
        setRequestBody(configurationObjectNode, payload);

        // Set the final URL in the configuration object
        configurationObjectNode.put(URL, constructEndpointsUrl(fullUrl, dictionaryName));
    }

    private String validateAndBuildFullUrl(String url, String path, NodeList formParametersNodeList) throws XPathExpressionException {
        path = validatePath(path);
        var fullUrl = url + path;

        var formParams = buildFormParameters(formParametersNodeList);
        if (!formParams.isEmpty()) {
            fullUrl += "?" + formParams;
        }

        return fullUrl;
    }

    private void setRequestBody(ObjectNode configurationObjectNode, String payload) {
        if (isNotNullOrEmpty(payload)) {
            payload = payload.replace("{", "").replace("}", ""); // Remove curly braces and extract it from the context
            configurationObjectNode.put(BODY, wrapValueInContextAttributes(payload));
        }
    }

    private String buildFormParameters(NodeList formParametersNodeList) throws XPathExpressionException {
        // If there are no form parameters, return an empty string
        if (formParametersNodeList == null || formParametersNodeList.getLength() == 0) {
            return "";
        }

        var formParams = new StringBuilder();

        for (int i = 0; i < formParametersNodeList.getLength(); i++) {
            var formParamNode = formParametersNodeList.item(i);
            processFormParameter(formParamNode, formParams);
        }

        return formParams.toString();
    }

    private void processFormParameter(Node formParamNode, StringBuilder formParams) throws XPathExpressionException {
        // Extract the name and value of the form parameter
        var paramName = xPath.evaluate("@name", formParamNode);
        var paramValue = replaceCurlyBraceAttributes(xPath.evaluate("text()", formParamNode));

        // If the formParams string is not empty, append an "&" before adding the new parameter
        if (!formParams.isEmpty()) {
            formParams.append("&");
        }
        formParams.append(paramName).append("=").append(paramValue);
    }

    private void processHeaders(NodeList headersNodeList, ArrayNode headersArray) throws XPathExpressionException {
        // If there are no headers, return immediately
        if (headersNodeList == null || headersNodeList.getLength() == 0) {
            return;
        }

        // Iterate through the headers and add them to the headers array
        for (int i = 0; i < headersNodeList.getLength(); i++) {
            var headerNode = headersNodeList.item(i);
            var headerName = xPath.evaluate("@name", headerNode);
            var headerValue = replaceCurlyBraceAttributes(xPath.evaluate("text()", headerNode));

            headersArray.addObject().put(NAME, headerName).put(VALUE, headerValue);
        }
    }

    private void configureResponseHandling(ObjectNode configurationObject, Document apiGeePolicy) throws XPathExpressionException {
        var responseName = xPath.evaluate("/ServiceCallout/Response", apiGeePolicy);

        if (responseName != null && !responseName.isEmpty()) {
            var variablesArray = configurationObject.putArray(VARIABLES);
            var variablesObject = variablesArray.addObject();
            variablesObject.put(NAME, responseName);
            variablesObject.put(VALUE, CALLOUT_RESPONSE_CONTENT);
            configurationObject.put(FIRE_AND_FORGET, false);
        } else {
            configurationObject.put(FIRE_AND_FORGET, true);
        }

        configurationObject.put(EXIT_ON_ERROR, true);
        configurationObject.put(ERROR_CONDITION, ERROR_CONDITION_EXPRESSION);
        configurationObject.put(ERROR_STATUS_CODE, String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    private String validatePath(String path) {
        // if path is not null or empty, and if it doesn't start with a slash, add a leading slash
        if (isNotNullOrEmpty(path) && !path.startsWith(PATH_DELIMITER)) {
            path = PATH_DELIMITER + path;
        }
        return path;
    }

    private String replaceCurlyBraceAttributes(String value) {
        // If the value is  a placeholder, remove the curly braces, and extract it from the context, else return the value as it is
        if (value.contains("{") && value.contains("}")) {
            String attributeKey = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
            return value.replace("{" + attributeKey + "}", wrapValueInContextAttributes(attributeKey));
        }
        return value;
    }
}