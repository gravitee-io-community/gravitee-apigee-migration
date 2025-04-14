package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.VERIFY_API_KEY;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.API_KEY;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.TRANSFORM_HEADERS;

/*
 * Converts VerifyAPIKey policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the VerifyAPIKey policy.
 */
@Component
@RequiredArgsConstructor
public class VerifyAPIKeyConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return VERIFY_API_KEY.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
        var policyName = xPath.evaluate("/VerifyAPIKey/@name", apiGeePolicy);
        var apiKeyRef = xPath.evaluate("/VerifyAPIKey/APIKey/@ref", apiGeePolicy);

        addTransformHeadersPolicy(stepNode, scopeArray, scope, policyName, apiKeyRef);
        addApiKeyPolicy(stepNode, scopeArray, policyName);
    }

    private void addTransformHeadersPolicy(Node stepNode, ArrayNode scopeArray, String scope, String policyName, String apiKeyRef) throws XPathExpressionException {
        var scopeObjectHeaders = createBaseScopeNode(stepNode, policyName, TRANSFORM_HEADERS, scopeArray);
        var configurationObjectHeaders = scopeObjectHeaders.putObject(CONFIGURATION);
        configurationObjectHeaders.put(SCOPE, scope.toUpperCase());

        var configurationsArray = configurationObjectHeaders.putArray(ADD_HEADERS);
        var configurationObject = configurationsArray.addObject();
        configurationObject.put(NAME, X_GRAVITEE_API_KEY);
        configurationObject.put(VALUE, "{#context.attributes['" + apiKeyRef + "']}");
    }

    private void addApiKeyPolicy(Node stepNode, ArrayNode scopeArray, String policyName) throws XPathExpressionException {
        var scopeObject = createBaseScopeNode(stepNode, policyName, API_KEY, scopeArray);
        var configurationObject = scopeObject.putObject(CONFIGURATION);
        configurationObject.put(PROPAGATE_API_KEY, false);
    }
}


