package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import com.gravitee.migration.util.constants.GraviteeCliConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Folder.RESOURCES;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.ON_REQUEST_SCRIPT;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.ON_RESPONSE_SCRIPT;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.JAVASCRIPT;

/**
 * <p>Converts JavaScript policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the JavaScript policy.</p>
 */
@Component
@RequiredArgsConstructor
public class JavaScriptConverter implements PolicyConverter {

    private final XPath xPath;
    private final FileReaderService fileReaderService;

    @Override
    public boolean supports(String policyType) {
        return JAVASCRIPT.equals(policyType);
    }

    /**
     * Converts the JavaScript policy from Apigee to Gravitee.
     *
     * @param condition     The condition to be applied to the policy.
     * @param apiGeePolicy   The policy document.
     * @param phaseArray     The array node to which the converted policy will be added (e.g., request, response).
     * @param phase          The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        // Extract the policy name and JavaScript content
        var policyName = xPath.evaluate("/Javascript/@name", apiGeePolicy);
        var javaScriptContent = extractJavaScriptContent(apiGeePolicy);

        createConfigurationForJavascript(condition, policyName, javaScriptContent, phaseArray, phase, conditionMappings);
    }

    private void createConfigurationForJavascript(String condition, String fileName, String javaScriptContent, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Create a base scope node for the JavaScript policy
        var objectNode = createBasePhaseObject(condition, fileName, GraviteeCliConstants.PolicyType.JAVASCRIPT, phaseArray, conditionMappings);
        var configurationObject = objectNode.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());

        // Set the phase and script content
        if (phase.equalsIgnoreCase(REQUEST)) {
            configurationObject.put(ON_REQUEST_SCRIPT, javaScriptContent);
        } else {
            configurationObject.put(ON_RESPONSE_SCRIPT, javaScriptContent);
        }
    }

    private String extractJavaScriptContent(Document apiGeePolicy) throws XPathExpressionException, IOException {
        // Find the correct folder we are located in and extract the JavaScript content that is present in the resources folder
        String resourcesPath = resolveResourcesPath();
        var resourceUrl = xPath.evaluate("/Javascript/ResourceURL", apiGeePolicy);
        var folderName = resourceUrl.substring(0, resourceUrl.indexOf(':'));
        var fileName = resourceUrl.substring(resourceUrl.indexOf("//") + 2);

        return getJavaScriptContent(resourcesPath, folderName, fileName);
    }

    private String resolveResourcesPath() {
        String currentFolder = fileReaderService.getCurrentFolder();
        Path childFolder = fileReaderService.findFirstChildFolder(currentFolder);
        return Paths.get(childFolder.toString(), RESOURCES).toString();
    }

    private String getJavaScriptContent(String resourcesPath, String folderName, String fileName) throws IOException {
        // If present in the resources folder, read the JavaScript file
        var sharedFlowResources = fileReaderService.parseJavaScriptFiles(resourcesPath, folderName);
        var jsContent = sharedFlowResources.get(fileName);

        if (jsContent == null) {
            throw new IllegalArgumentException("No matching JavaScript file found for: " + fileName);
        }
        return jsContent;
    }
}
