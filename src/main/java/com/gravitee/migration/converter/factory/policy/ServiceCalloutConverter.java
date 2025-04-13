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

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.StringUtils.constructEndpointsUrl;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.SERVICE_CALLOUT;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.HTTP_CALLOUT;

/**
 * Converts ServiceCallout policy from APIgee to Gravitee.
 * Implements the PolicyConverter interface and provides the logic to convert the ServiceCallout policy.
 */
@Component
@RequiredArgsConstructor
public class ServiceCalloutConverter implements PolicyConverter {

    private final XPath xPath;
    private static final String PATH_DELIMITER = "/";

    @Override
    public boolean supports(String policyType) {
        return SERVICE_CALLOUT.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws XPathExpressionException {
        var policyName = xPath.evaluate("/ServiceCallout/@name", apiGeePolicy);
        var headersNodeList = (NodeList) xPath.evaluate("/ServiceCallout/Request/Set/Headers/Header", apiGeePolicy, XPathConstants.NODESET);
        var formParametersNodeList = (NodeList) xPath.evaluate("/ServiceCallout/Request/Set/FormParams/FormParam", apiGeePolicy, XPathConstants.NODESET);
        var method = xPath.evaluate("/ServiceCallout/Request/Set/Verb", apiGeePolicy);
        var path = xPath.evaluate("/ServiceCallout/Request/Set/Path", apiGeePolicy);
        var url = xPath.evaluate("/ServiceCallout/HTTPTargetConnection/URL", apiGeePolicy);


        var scopeObjectNode = createBaseScopeNode(stepNode, policyName, HTTP_CALLOUT, scopeArray);
        var configurationObjectNode = scopeObjectNode.putObject(CONFIGURATION);

        configureRequest(configurationObjectNode, method, url, path, formParametersNodeList);
        processHeaders(headersNodeList, configurationObjectNode.putArray("headers"));
        configureResponseHandling(scopeObjectNode, apiGeePolicy);
    }

    private void configureRequest(ObjectNode configurationObjectNode, String method, String url, String path, NodeList formParametersNodeList) throws XPathExpressionException {
        path = validatePath(path);
        var fullUrl = url + path;
        var formParams = buildFormParameters(formParametersNodeList);

        if (!formParams.isEmpty()) {
            fullUrl += "?" + formParams;
        }

        configurationObjectNode.put(METHOD, (method != null && !method.isEmpty()) ? method : "GET");
        configurationObjectNode.put(URL, constructEndpointsUrl(fullUrl));
    }

    private String buildFormParameters(NodeList formParametersNodeList) throws XPathExpressionException {
        var formParams = new StringBuilder();

        for (int i = 0; i < formParametersNodeList.getLength(); i++) {
            var formParamNode = formParametersNodeList.item(i);
            var paramName = xPath.evaluate("@name", formParamNode);
            var paramValue = replaceCurlyBraceAttributes(xPath.evaluate("text()", formParamNode));

            if (!formParams.isEmpty()) {
                formParams.append("&");
            }
            formParams.append(paramName).append("=").append(paramValue);
        }

        return formParams.toString();
    }

    private void processHeaders(NodeList headersNodeList, ArrayNode headersArray) throws XPathExpressionException {
        for (int i = 0; i < headersNodeList.getLength(); i++) {
            var headerNode = headersNodeList.item(i);
            var headerName = xPath.evaluate("@name", headerNode);
            var headerValue = replaceCurlyBraceAttributes(xPath.evaluate("text()", headerNode));

            headersArray.addObject().put(NAME, headerName).put(VALUE, headerValue);
        }
    }

    private void configureResponseHandling(ObjectNode scopeObjectNode, Document apiGeePolicy) throws XPathExpressionException {
        var variablesArray = scopeObjectNode.putArray(VARIABLES);
        var responseName = xPath.evaluate("/ServiceCallout/Response", apiGeePolicy);

        if (responseName != null && !responseName.isEmpty()) {
            var variablesObject = variablesArray.addObject();
            variablesObject.put(NAME, responseName);
            variablesObject.put(VALUE, "{#calloutResponse.content}");
            scopeObjectNode.put(FIRE_AND_FORGET, false);
        } else {
            scopeObjectNode.put(FIRE_AND_FORGET, true);
        }

        scopeObjectNode.put(EXIT_ON_ERROR, true);
        scopeObjectNode.put(ERROR_CONDITION, "{#calloutResponse.status >= 400 and #calloutResponse.status <= 599}");
        scopeObjectNode.put(ERROR_STATUS_CODE, 500);
    }

    private String validatePath(String path) {
        if (path != null && !path.startsWith(PATH_DELIMITER)) {
            path = PATH_DELIMITER + path;
        }
        return path;
    }

    private String replaceCurlyBraceAttributes(String value) {
        if (value.contains("{") && value.contains("}")) {
            String attributeKey = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
            return value.replace("{" + attributeKey + "}", "{#context.attributes['" + attributeKey + "']}");
        }
        return value;
    }
}