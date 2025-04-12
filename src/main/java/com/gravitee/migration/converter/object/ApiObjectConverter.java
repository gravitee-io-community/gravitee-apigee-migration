package com.gravitee.migration.converter.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashMap;
import java.util.UUID;

import static com.gravitee.migration.util.StringUtils.removeNewlines;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Api.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;

/**
 * This class is responsible for mapping fields from the Apigee XML to the api object in the Gravitee JSON.
 */
@Component
@RequiredArgsConstructor
public class ApiObjectConverter {

    private final ObjectMapper objectMapper;
    private final XPath xPath;
    private ArrayNode resourcesArray;

    /**
     * Creates the api object in the Gravitee JSON, with values mapped from Apigee XML.
     *
     * @param rootXml        the main xml document containing the <APIProxy> tag, located in the apiproxy folder
     * @param proxyXml       the document containing the proxy information located in proxies folder
     * @param graviteeConfig the root Gravitee JSON object
     */
    public void mapApiObject(Document rootXml, Document proxyXml, ObjectNode graviteeConfig) throws XPathExpressionException {
        var apiNode = graviteeConfig.putObject(API_OBJECT);
        // Hardcoded values for Gravitee
        apiNode.put(DEFINITION_VERSION, V4);
        apiNode.put(TYPE, PROXY.toUpperCase());
        apiNode.put(ID, UUID.randomUUID().toString());

        // Name of the API in gravitee - mapped from rootXml 'name' attribute inside <APIProxy> tag
        var apiName = xPath.evaluate("/APIProxy/@name", rootXml);
        apiNode.put(NAME, !apiName.isEmpty() ? apiName : "Unnamed API");

        // Description of the API in gravitee - mapped from rootXml <Description> tag
        var description = xPath.evaluate("/APIProxy/Description", rootXml);
        apiNode.put(DESCRIPTION, !description.isEmpty() ? removeNewlines(description) : "Migrated from Apigee");

        // Version of the API in gravitee - mapped from rootXml 'majorVersion' and 'minorVersion' attributes inside <ConfigurationVersion> tag
        var majorVersion = xPath.evaluate("/APIProxy/ConfigurationVersion/@majorVersion", rootXml);
        var minorVersion = xPath.evaluate("/APIProxy/ConfigurationVersion/@minorVersion", rootXml);
        apiNode.put(API_VERSION, majorVersion + "." + minorVersion);

        // Created and updated timestamps of the API in gravitee - mapped from rootXml <CreatedAt> and <LastModifiedAt> tags
        var createdAtMillis = xPath.evaluate("/APIProxy/CreatedAt", rootXml);
        var updatedAtMillis = xPath.evaluate("/APIProxy/LastModifiedAt", rootXml);
        apiNode.put(CREATED_AT, DateUtils.convertMillisToIso8601(Long.parseLong(createdAtMillis)));
        apiNode.put(UPDATED_AT, DateUtils.convertMillisToIso8601(Long.parseLong(updatedAtMillis)));

        mapListeners(proxyXml, apiNode);
        mapEndpointGroups(apiNode);

        resourcesArray = objectMapper.createArrayNode();
        apiNode.set("resources", resourcesArray);
    }

    public void buildResources(Document apiGeePolicy) throws XPathExpressionException {
        // Extract the name of the cache resource from the policy
        var cacheName = xPath.evaluate("/*/CacheResource", apiGeePolicy);

        // Check if an object with the same name already exists in the resources array
        checkForExistingResource(cacheName);

        // Create a new resource object if no match is found
        var resourceObject = resourcesArray.addObject();
        resourceObject.put(NAME, cacheName);
        resourceObject.put(TYPE, "cache");
        resourceObject.put(ENABLED, true);

        // Create the configuration as a JSON string - default values(not present in Apigee)
        var configurationMap = new HashMap<>();
        configurationMap.put("timeToIdleSeconds", 0);
        configurationMap.put("timeToLiveSeconds", 0);
        configurationMap.put("maxEntriesLocalHeap", 1000);

        String configurationJson;
        try {
            configurationJson = objectMapper.writeValueAsString(configurationMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert configuration to JSON", e);
        }

        resourceObject.put("configuration", configurationJson);
    }

    /**
     * Checks if an object with the same name already exists in the resources array.
     *
     * @param resourceName the name of the resource
     */
    private void checkForExistingResource(String resourceName) {
        for (var resourceNode : resourcesArray) {
            if (resourceNode.has(NAME) && resourceName.equals(resourceNode.get(NAME).asText())) {
                // Object with the same name already exists, skip creation
                return;
            }
        }
    }

    /**
     * Creates the listeners array from the Apigee XML to the Gravitee JSON.
     *
     * @param proxyXml the document containing the proxy information located in proxies folder
     * @param apiNode  the api object in the Gravitee JSON
     */
    private void mapListeners(Document proxyXml, ObjectNode apiNode) throws XPathExpressionException {
        var listenersArray = apiNode.putArray(LISTENERS);
        var listenerNode = listenersArray.addObject();
        // Hardcoded value for Gravitee
        listenerNode.put(TYPE, HTTP);

        constructListenerPaths(listenerNode, proxyXml);
        constructListenerEntryPoints(listenerNode);
    }

    /**
     * Creates the paths array inside the listener object in the Gravitee JSON.
     *
     * @param listenerNode the listener object in the Gravitee JSON
     * @param proxyXml     the document containing the proxy information located in proxies folder
     */
    private void constructListenerPaths(ObjectNode listenerNode, Document proxyXml) throws XPathExpressionException {
        var pathsArray = listenerNode.putArray(PATHS);
        var pathNode = pathsArray.addObject();

        // Extracted from the proxyXml document <BasePath> tag
        pathNode.put(PATH, xPath.evaluate("/ProxyEndpoint/HTTPProxyConnection/BasePath", proxyXml));
        // Hardcoded values for Gravitee
        pathNode.put(OVERRIDE_ACCESS, false);
        // Extracted from the proxyXml document <VirtualHost> tag
        var host = xPath.evaluate("/ProxyEndpoint/HTTPProxyConnection/VirtualHost", proxyXml);
        if (!host.isEmpty()) {
            pathNode.put(HOST, xPath.evaluate("/ProxyEndpoint/HTTPProxyConnection/VirtualHost", proxyXml));
        }
    }

    /**
     * Creates the entrypoints array inside the listener object in the Gravitee JSON.
     *
     * @param listenerNode the listener object in the Gravitee JSON
     */
    private void constructListenerEntryPoints(ObjectNode listenerNode) {
        var entryPointsArray = listenerNode.putArray(ENTRYPOINTS);
        var entrypointNode = entryPointsArray.addObject();

        // Hardcoded values for Gravitee
        //! CHECK IF IT IS REQUIRED OR WHERE IT COMES FROM
        entrypointNode.put(TYPE, HTTP_PROXY);
        entrypointNode.put(QOS, AUTO);
        entrypointNode.putObject(CONFIGURATION);
    }

    private void mapEndpointGroups(ObjectNode apiNode) throws XPathExpressionException {
        var endpointGroupsArray = apiNode.putArray(ENDPOINT_GROUPS);
        var endpointGroupNode = endpointGroupsArray.addObject();

        // Extracts the name of the target endpoint from the targetXml document @name attribute located in the <TargetEndpoint> tag
        endpointGroupNode.put(NAME, DEFAULT_ENDPOINT);
        // Hardcoded values for Gravitee
        endpointGroupNode.put(TYPE, HTTP_PROXY);
        //! CHECK SERVICES
        endpointGroupNode.putObject(SERVICES);

        // Hardcoded values for Gravitee
        var loadBalancerOptionsNode = endpointGroupNode.putObject(LOAD_BALANCER);
        loadBalancerOptionsNode.put(TYPE, ROUND_ROBIN);

        constructEndpointGroupSharedConfiguration(endpointGroupNode);
        constructEndpointGroupsEndpoints(endpointGroupNode);
    }

    private void constructEndpointGroupSharedConfiguration(ObjectNode endpointGroupNode) {
        var sharedConfigurationNode = endpointGroupNode.putObject(SHARED_CONFIGURATION);
        configureSsl(sharedConfigurationNode);
    }

    private void constructEndpointGroupsEndpoints(ObjectNode endpointGroupNode) {
        var endpointsArray = endpointGroupNode.putArray(ENDPOINTS);

        // For each target endpoint in apiGee, create an endpoint in Gravitee
            var endpointNode = endpointsArray.addObject();
            endpointNode.put(NAME, "Target Backend URL");
            endpointNode.put(TYPE, HTTP_PROXY);
            endpointNode.put(WEIGHT, 1);
            endpointNode.put(INHERIT_CONFIGURATION, true);
            endpointNode.putObject(CONFIGURATION).put(TARGET, "https://changeMe");
            endpointNode.putObject(SERVICES);
            endpointNode.put(SECONDARY, false);

    }

    private void configureSsl(ObjectNode sharedConfigurationNode) {
        // Hardcoded values for Gravitee
        var sslNode = sharedConfigurationNode.putObject(SSL);
        sslNode.put(TRUST_ALL, false);
        sslNode.put(HOST_NAME_VERIFIER, true);
        sslNode.putObject(KEY_STORE).put(TYPE, "");
        sslNode.putObject(TRUST_STORE).put(TYPE, "");
    }
}