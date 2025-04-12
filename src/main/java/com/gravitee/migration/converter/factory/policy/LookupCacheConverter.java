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
import static com.gravitee.migration.util.StringUtils.buildKeyFragmentString;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.LOOKUP_CACHE;

/**
 * Converts LookupCache policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the LookupCache policy.
 */
@Component
@RequiredArgsConstructor
public class LookupCacheConverter implements PolicyConverter {

    private final XPath xPath;

    public boolean supports(String policyType) {
        return LOOKUP_CACHE.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws XPathExpressionException {
        var policyName = xPath.evaluate("/LookupCache/@name", apiGeePolicy);
        var cacheResource = xPath.evaluate("/LookupCache/CacheResource", apiGeePolicy);
        var cacheKeyFragments = (NodeList) xPath.evaluate("/PopulateCache/CacheKey/KeyFragment", apiGeePolicy, XPathConstants.NODESET);
        var key = buildKeyFragmentString(cacheKeyFragments);
        var assignTo = xPath.evaluate("/LookupCache/AssignTo", apiGeePolicy);

        var objectNode = createBaseScopeNode(stepNode, policyName, "data-cache", scopeArray);
        configureCache(objectNode, cacheResource, key, assignTo);
    }


    private void configureCache(ObjectNode objectNode, String cacheResource, String key, String assignTo) {
        var configurationObject = objectNode.putObject("configuration");
        configurationObject.put("resource", cacheResource);
        configurationObject.put("cacheKey", key);
        configurationObject.put("timeToLive", 3600); // Default TTL, where do we extract this from?
        configurationObject.put("defaultOperation", "GET");
        configurationObject.put("value", assignTo);
    }
}
