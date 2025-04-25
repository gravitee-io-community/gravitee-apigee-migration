package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.wrapValueInContextAttributes;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.*;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.VERIFY_API_KEY;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.API_KEY;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.TRANSFORM_HEADERS;

/**
 * <p>Converts VerifyAPIKey policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the VerifyAPIKey policy.</p>
 */
@Component
@RequiredArgsConstructor
public class VerifyAPIKeyConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return VERIFY_API_KEY.equals(policyType);
    }

    /**
     * Converts the VerifyAPIKey policy from Apigee to Gravitee.
     * This method extracts the necessary values from the Apigee policy and constructs the corresponding Gravitee policy.
     * By default, the policy will look for the API key inside the X-Gravitee-Api-Key header.
     *
     * @param condition     The condition to be applied to the policy.
     * @param apiGeePolicy The Apigee policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        // Extract values
        var policyName = xPath.evaluate("/VerifyAPIKey/@name", apiGeePolicy);
        var apiKeyRef = xPath.evaluate("/VerifyAPIKey/APIKey/@ref", apiGeePolicy);

        addTransformHeadersPolicy(condition, phaseArray, phase, policyName, apiKeyRef, conditionMappings);
        addApiKeyPolicy(condition, phaseArray, policyName, conditionMappings);
    }

    private void addTransformHeadersPolicy(String condition, ArrayNode phaseArray, String phase, String policyName, String apiKeyRef, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Extract the API key from the context and set it in the X-Gravitee-Api-Key header
        var scopeObjectHeaders = createBasePhaseObject(condition, policyName.concat("-Set-API-Key-Header"), TRANSFORM_HEADERS, phaseArray, conditionMappings);
        var configurationObjectHeaders = scopeObjectHeaders.putObject(CONFIGURATION);
        configurationObjectHeaders.put(SCOPE, phase.toUpperCase());
        scopeObjectHeaders.put(ENABLED, false);

        var configurationsArray = configurationObjectHeaders.putArray(ADD_HEADERS);
        var configurationObject = configurationsArray.addObject();
        configurationObject.put(NAME, X_GRAVITEE_API_KEY);
        configurationObject.put(VALUE, wrapValueInContextAttributes(apiKeyRef));
    }

    private void addApiKeyPolicy(String condition, ArrayNode phaseArray, String policyName, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Create API Key policy
        var scopeObject = createBasePhaseObject(condition, policyName, API_KEY, phaseArray, conditionMappings);
        var configurationObject = scopeObject.putObject(CONFIGURATION);
        configurationObject.put(PROPAGATE_API_KEY, false);
    }
}


