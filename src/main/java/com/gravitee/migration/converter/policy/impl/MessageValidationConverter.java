package com.gravitee.migration.converter.policy.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.policy.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.GraviteeCliUtils.createGroovyConfiguration;
import static com.gravitee.migration.util.StringUtils.readGroovyPolicy;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.MESSAGE_VALIDATION;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.GROOVY;

/**
 * <p>Converts MessageValidation policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the MessageValidation policy.</p>
 */
@Component
@RequiredArgsConstructor
public class MessageValidationConverter implements PolicyConverter {

    @Value("${groovy.message-validation}")
    private String messageValidationGroovyFileLocation;

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return MESSAGE_VALIDATION.equals(policyType);
    }

    /**
     * Converts the MessageValidation policy from Apigee to Gravitee.
     * This policy only validates the body of the XML or JSON, if it is not valid it sets attribute "messagevalidation.failed" == true
     * in the context, which can be later used as a condition check.
     *
     * @param condition   The condition to be applied to the policy.
     * @param apiGeePolicy   The policy document.
     * @param phaseArray     The array node to which the converted policy will be added (e.g., request, response).
     * @param phase          The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        // Extract the policy name
        var policyName = xPath.evaluate("/MessageValidation/@name", apiGeePolicy);
        // Create a base phase node for the policy
        var phaseObject = createBasePhaseObject(condition, policyName, GROOVY, phaseArray, conditionMappings);
        // Read the Groovy script from the specified text file
        var policyString = readGroovyPolicy(messageValidationGroovyFileLocation);
        // Create the groovy configuration
        createGroovyConfiguration(policyString, phase, phaseObject);
    }
}
