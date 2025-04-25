package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.buildCacheKey;
import static com.gravitee.migration.util.StringUtils.readGroovyPolicy;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.CACHE_KEY;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.DEFAULT_OPERATION;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.LOOKUP_CACHE;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.DATA_CACHE;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.GROOVY;

/**
 * <p>Converts LookupCache policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the LookupCache policy.</p>
 */
@Component
@RequiredArgsConstructor
public class LookupCacheConverter implements PolicyConverter {

    @Value("${groovy.lookup-cache}")
    private String lookupCacheGroovyFileLocation;

    private final XPath xPath;

    public boolean supports(String policyType) {
        return LOOKUP_CACHE.equals(policyType);
    }
    /**
     * Converts the LookupCache policy from Apigee to Gravitee.
     *
     * @param condition    The condition to be applied to the policy.
     * @param apiGeePolicy The Apigee policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws XPathExpressionException if an error occurs during XPath evaluation.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException, IOException {
        // Extract properties
        var policyName = xPath.evaluate("/LookupCache/@name", apiGeePolicy);
        var cacheResource = xPath.evaluate("/LookupCache/CacheResource", apiGeePolicy);
        var cacheKeyFragments = (NodeList) xPath.evaluate("/LookupCache/CacheKey/KeyFragment", apiGeePolicy, XPathConstants.NODESET);
        var prefix = xPath.evaluate("/LookupCache/CacheKey/Prefix", apiGeePolicy);

        var key = buildCacheKey(cacheKeyFragments, prefix);
        var assignTo = xPath.evaluate("/LookupCache/AssignTo", apiGeePolicy);

        var objectNode = createBasePhaseObject(condition, policyName, DATA_CACHE, phaseArray, conditionMappings);
        configureCache(objectNode, cacheResource, key);
        addExtractedKeyToContext(condition, policyName, phaseArray, phase, key, assignTo, conditionMappings);
    }

    private void addExtractedKeyToContext(String condition, String policyName, ArrayNode phaseArray, String phase, String key, String assignTo, Map<String, String> conditionMappings) throws XPathExpressionException, IOException {
        // Creates a groovy policy to store the extracted key in the context
        var scopeObject = createBasePhaseObject(condition, policyName.concat("-Set-Attribute"), GROOVY, phaseArray, conditionMappings);
        var configurationObject = createConfigurationObject(scopeObject, phase);
        var script = generateScript(key, assignTo, policyName);

        configurationObject.put(SCRIPT, script);
    }

    private ObjectNode createConfigurationObject(ObjectNode scopeObject, String phase) {
        var configurationObject = scopeObject.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());
        configurationObject.put(READ_CONTENT, true);
        configurationObject.put(OVERRIDE_CONTENT, false);

        return configurationObject;
    }

    private String generateScript(String key, String assignTo, String policyName) throws IOException {
        // Executes a Groovy script to cache the extracted value in the context under a specified name.
        // By default, Data-Cache policy in Gravitee caches the value with the name of the extracted key. A custom script is required
        // to store the value with a different name.
        var policyTemplate = readGroovyPolicy(lookupCacheGroovyFileLocation);

        return String.format(policyTemplate, key, assignTo, policyName);
    }

    private void configureCache(ObjectNode objectNode, String cacheResource, String key) {
        // Configure the cache resource that will extract the value based on the key
        var configurationObject = objectNode.putObject(CONFIGURATION);
        configurationObject.put(RESOURCE, cacheResource);
        configurationObject.put(CACHE_KEY, key);
        configurationObject.put(DEFAULT_OPERATION, HttpMethod.GET.name());
    }
}
