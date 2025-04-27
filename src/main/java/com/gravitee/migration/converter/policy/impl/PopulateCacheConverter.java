package com.gravitee.migration.converter.policy.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.policy.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.buildCacheKey;
import static com.gravitee.migration.util.StringUtils.wrapValueInContextAttributes;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.CACHE_KEY;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.DEFAULT_OPERATION;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.POPULATE_CACHE;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.DATA_CACHE;

/**
 * <p>Converts PopulateCache policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the PopulateCache policy.</p>
 */
@Component
@RequiredArgsConstructor
public class PopulateCacheConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return POPULATE_CACHE.equals(policyType);
    }

    /**
     * Converts the PopulateCache policy from Apigee to Gravitee.
     *
     * @param condition   The condition to be applied to the policy.
     * @param apiGeePolicy The policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        // Extract properties
        var policyName = xPath.evaluate("/PopulateCache/@name", apiGeePolicy);
        var cacheResourceName = xPath.evaluate("/PopulateCache/CacheResource", apiGeePolicy);
        var cacheKeyFragments = (NodeList) xPath.evaluate("/PopulateCache/CacheKey/KeyFragment", apiGeePolicy, XPathConstants.NODESET);
        var prefix = xPath.evaluate("/PopulateCache/CacheKey/Prefix", apiGeePolicy);

        var key = buildCacheKey(cacheKeyFragments, prefix);

        var objectNode = createBasePhaseObject(condition, policyName, DATA_CACHE, phaseArray, conditionMappings);
        configureCache(objectNode, cacheResourceName, key, apiGeePolicy);
    }

    private void configureCache(ObjectNode objectNode, String cacheResourceName, String key, Document apiGeePolicy) throws XPathExpressionException {
        // Create data-cache policy which sets a value in the cache
        var configurationObject = objectNode.putObject(CONFIGURATION);
        configurationObject.put(RESOURCE, cacheResourceName);
        configurationObject.put(CACHE_KEY, key);
        configurationObject.put(DEFAULT_OPERATION, "SET");
        configureSourceValue(configurationObject, apiGeePolicy);
    }

    private void configureSourceValue(ObjectNode configurationObject, Document apiGeePolicy) throws XPathExpressionException {
        var sourceValue = xPath.evaluate("/PopulateCache/Source", apiGeePolicy);
        // value that will be set in the cache
        configurationObject.put(VALUE, wrapValueInContextAttributes(sourceValue));
    }
}