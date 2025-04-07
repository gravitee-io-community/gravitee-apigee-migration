package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.enums.RateLimitIntervalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.CONFIGURATION;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.SPIKE_ARREST;

@Component
@RequiredArgsConstructor
public class SpikeArrestMapper implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return SPIKE_ARREST.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray) throws XPathExpressionException {
        var name = xPath.evaluate("/SpikeArrest/@name", apiGeePolicy);
        var scopeNode = createBaseScopeNode(stepNode, name, "spike-arrest", scopeArray);

        constructRequestConfiguration(scopeNode, apiGeePolicy);
    }

    private void constructRequestConfiguration(ObjectNode requestNode, Document apiGeePolicy) throws XPathExpressionException {
        var configurationNode = requestNode.putObject(CONFIGURATION);
        var async = xPath.evaluate("/SpikeArrest/@async", apiGeePolicy);

        configurationNode.put("async", Boolean.parseBoolean(async));
        configurationNode.put("addHeaders", true);
        constructRequestRateLimit(configurationNode, apiGeePolicy);
    }

    private void constructRequestRateLimit(ObjectNode configurationNode, Document apiGeePolicy) throws XPathExpressionException {
        var spikeNode = configurationNode.putObject("spike");
        var rate = xPath.evaluate("/SpikeArrest/Rate", apiGeePolicy);

        var limit = Integer.parseInt(rate.replaceAll("\\D+", "")); // Extract only the numeric part
        spikeNode.put("limit", limit);

        var rateString = rate.replaceAll("\\d+", ""); // Extract only the string part
        spikeNode.put("periodTime", RateLimitIntervalMapper.mapRateToInt(rateString));
        spikeNode.put("periodTimeUnit", RateLimitIntervalMapper.mapRate(rateString));

        var identifierRef = xPath.evaluate("/SpikeArrest/Identifier/@ref", apiGeePolicy);
        if (identifierRef.equals("request.header.Origin")) {
            spikeNode.put("key", "{#request.headers['origin']}");
        }
    }
}
