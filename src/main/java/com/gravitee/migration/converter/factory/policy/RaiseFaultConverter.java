package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.isNotNullOrEmpty;
import static com.gravitee.migration.util.StringUtils.readGroovyPolicy;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.RAISE_FAULT;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.GROOVY;

/**
 * <p>This class is responsible for converting the RaiseFault policy from the old format to the new format.</p>
 *
 * <p>It implements the PolicyConverter interface and provides the logic
 * for converting the RaiseFault policy.</p>
 */
@Component
@RequiredArgsConstructor
public class RaiseFaultConverter implements PolicyConverter {

    @Value("${groovy.raise-fault}")
    private String raiseFaultGroovyFileLocation;

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return RAISE_FAULT.equals(policyType);
    }

    /**
     * Converts the RaiseFault policy from Apigee to Gravitee.
     * This policy throws a fault with a specified status code and payload, when a condition is met.
     *
     * @param condition    The condition to be applied to the policy.
     * @param apiGeePolicy The policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g, request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        // Extract values
        var policyName = xPath.evaluate("/RaiseFault/@name", apiGeePolicy);
        var status = xPath.evaluate("/RaiseFault/FaultResponse/Set/StatusCode", apiGeePolicy);
        var payloadHeader = xPath.evaluate("/RaiseFault/FaultResponse/Set/Payload/@contentType", apiGeePolicy);
        var payload = xPath.evaluate("/RaiseFault/FaultResponse/Set/Payload/text()", apiGeePolicy);

                // Create and configure the scope object
                createAndConfigurePhaseObject(condition, policyName, phaseArray, phase, status, payloadHeader, payload, conditionMappings, apiGeePolicy);
    }

    private void createAndConfigurePhaseObject(String condition, String policyName, ArrayNode phaseArray, String phase,
                                               String status, String payloadHeader, String payload, Map<String, String> conditionMappings, Document apiGeePolicy) throws Exception {
        // Create a base scope node for the policy
        var scopeObject = createBasePhaseObject(condition, policyName, GROOVY, phaseArray, conditionMappings);
        var configurationObject = scopeObject.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());
        configurationObject.put(SCRIPT, constructGroovyPolicy(status, payloadHeader, payload, apiGeePolicy));
    }


    private String constructGroovyPolicy(String status, String payloadHeader, String payload, Document apiGeePolicy) throws Exception {
        // Set default status if none is provided or if it is empty
        status = isNotNullOrEmpty(status)
                ? status
                : String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());

        // Set payload as reasonPhase if none is provided or if it is empty
        payload = isNotNullOrEmpty(payload)
                ? payload
                : xPath.evaluate("/RaiseFault/FaultResponse/Set/ReasonPhrase", apiGeePolicy);

        // Read the Groovy script from the specified text file
        var policyTemplate = readGroovyPolicy(raiseFaultGroovyFileLocation);

        // Replace placeholders in the Groovy script with actual values
        return String.format(policyTemplate, status, payload, payloadHeader);
    }
}
