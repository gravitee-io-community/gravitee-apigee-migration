package com.gravitee.migration.converter.policy.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.policy.PolicyConverter;
import com.gravitee.migration.enums.RateLimitIntervalMapper;
import com.gravitee.migration.util.constants.policy.PolicyTypeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.wrapValueInContextAttributes;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.ADD_HEADERS;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.SPIKE;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.SPIKE_ARREST;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.ASSIGN_ATTRIBUTES;

/**
 * <p>Converts the SpikeArrest policy from Apigee to Gravitee format.</p>
 *
 * <p>Implements the PolicyConverter interface and provides the logic
 *  to convert the SpikeArrestMapper policy.</p>
 */
@Component
@RequiredArgsConstructor
public class SpikeArrestConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return SPIKE_ARREST.equals(policyType);
    }

    /**
     * Converts the SpikeArrest policy from Apigee to Gravitee.
     *
     * @param condition The condition to be applied to the policy.
     * @param apiGeePolicy The Apigee policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws XPathExpressionException if an error occurs during XPath evaluation.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Extract properties
        var policyName = xPath.evaluate("/SpikeArrest/@name", apiGeePolicy);
        var scopeNode = createBasePhaseObject(condition, policyName, PolicyTypeConstants.SPIKE_ARREST, phaseArray, conditionMappings);
        var identifier = xPath.evaluate("/SpikeArrest/Identifier/@ref", apiGeePolicy);

        constructRequestConfiguration(scopeNode, apiGeePolicy);
        addAttributeToContext(condition, policyName, identifier, phaseArray, conditionMappings, phase);
    }

    private void addAttributeToContext(String condition, String policyName, String identifier, ArrayNode phaseArray, Map<String, String> conditionMappings, String phase) {
    var phaseObject = createBasePhaseObject(condition, policyName.concat("-Set-Differentiator"), ASSIGN_ATTRIBUTES, phaseArray, conditionMappings);
    var configurationObject = phaseObject.putObject(CONFIGURATION);
    configurationObject.put(SCOPE, phase.toUpperCase());

    var attributesArray = configurationObject.putArray(ATTRIBUTES);
    var attributeNode = attributesArray.addObject();
    attributeNode.put(NAME, "SpikeArrest.Differentiator");
    attributeNode.put(VALUE, wrapValueInContextAttributes(identifier));
    }

    private void constructRequestConfiguration(ObjectNode requestNode, Document apiGeePolicy) throws XPathExpressionException {
        // Create configuration
        var configurationNode = requestNode.putObject(CONFIGURATION);
        var async = xPath.evaluate("/SpikeArrest/@async", apiGeePolicy);

        configurationNode.put(ASYNC, Boolean.parseBoolean(async));
        configurationNode.put(ADD_HEADERS, true);
        constructRequestRateLimit(configurationNode, apiGeePolicy);
    }

    private void constructRequestRateLimit(ObjectNode configurationNode, Document apiGeePolicy) throws XPathExpressionException {
        var spikeNode = configurationNode.putObject(SPIKE);
        var rate = xPath.evaluate("/SpikeArrest/Rate", apiGeePolicy);

        var limit = Integer.parseInt(rate.replaceAll("\\D+", "")); // Extract only the numeric part
        spikeNode.put(LIMIT, limit);

        var rateString = rate.replaceAll("\\d+", ""); // Extract only the string part
        spikeNode.put(PERIOD_TIME, RateLimitIntervalMapper.mapShorthandRateToInt(rateString));
        spikeNode.put(PERIOD_TIME_UNIT, RateLimitIntervalMapper.mapShorthandRate(rateString));

        var identifierRef = xPath.evaluate("/SpikeArrest/Identifier/@ref", apiGeePolicy);
        // Wrap the identifierRef value in context attributes
        if (identifierRef.equals("request.header.Origin")) {
            spikeNode.put(KEY, String.format(REQUEST_HEADER_WRAPPED, "origin"));
        }
    }
}
