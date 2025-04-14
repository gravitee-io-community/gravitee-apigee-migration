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

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.StringUtils.addPrefixToKey;
import static com.gravitee.migration.util.StringUtils.buildKeyFragmentString;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.POPULATE_CACHE;

@Component
@RequiredArgsConstructor
public class PopulateCacheConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return POPULATE_CACHE.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws Exception {
        var policyName = xPath.evaluate("/PopulateCache/@name", apiGeePolicy);
        var cacheResourceName = xPath.evaluate("/PopulateCache/CacheResource", apiGeePolicy);
        var cacheKeyFragments = (NodeList) xPath.evaluate("/PopulateCache/CacheKey/KeyFragment", apiGeePolicy, XPathConstants.NODESET);
        var prefix = xPath.evaluate("/PopulateCache/CacheKey/Prefix", apiGeePolicy);
        var key = buildKeyFragmentString(cacheKeyFragments);

        // Add prefix to the key if it exists
        key = addPrefixToKey(prefix, key);

        var objectNode = createBaseScopeNode(stepNode, policyName, "data-cache", scopeArray);
        configureCache(objectNode, cacheResourceName, key, apiGeePolicy);
    }

    private void configureCache(ObjectNode objectNode, String cacheResourceName, String key, Document apiGeePolicy) throws Exception {
        var configurationObject = objectNode.putObject("configuration");
        configurationObject.put("resource", cacheResourceName);
        configurationObject.put("cacheKey", key);
        configurationObject.put("defaultOperation", "SET");
        configureSourceValue(configurationObject, apiGeePolicy);
    }

    private void configureSourceValue(ObjectNode configurationObject, Document apiGeePolicy) throws Exception {
        var sourceValue = xPath.evaluate("/PopulateCache/Source", apiGeePolicy);
        configurationObject.put("value", "{#context.attributes['" + sourceValue + "']}");
    }
}