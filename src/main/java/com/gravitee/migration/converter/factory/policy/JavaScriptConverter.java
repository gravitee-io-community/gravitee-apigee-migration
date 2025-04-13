package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import com.gravitee.migration.util.constants.GraviteeCliConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.CONFIGURATION;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.REQUEST;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Folder.RESOURCES;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.JAVASCRIPT;

/**
 * Converts JavaScript policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the JavaScript policy.
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

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws Exception {
        var policyName = xPath.evaluate("/Javascript/@name", apiGeePolicy);
        var document = extractJavaScriptContent(apiGeePolicy);

        createConfigurationForJavascript(stepNode, policyName, document, scopeArray, phase);
    }

    private void createConfigurationForJavascript(Node stepNode, String fileName, String javaScriptContent, ArrayNode scopeArray, String phase) throws XPathExpressionException {
        var objectNode = createBaseScopeNode(stepNode, fileName, GraviteeCliConstants.PolicyType.JAVASCRIPT, scopeArray);
        var configurationObject = objectNode.putObject(CONFIGURATION);

        if (phase.equalsIgnoreCase(REQUEST)) {
            configurationObject.put("onRequestScript", javaScriptContent);
        } else {
            configurationObject.put("onResponseScript", javaScriptContent);
        }
    }

    private String extractJavaScriptContent(Document apiGeePolicy) throws XPathExpressionException, IOException {
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
        var sharedFlowResources = fileReaderService.parseJavaScriptFiles(resourcesPath, folderName);
        var jsContent = sharedFlowResources.get(fileName);

        if (jsContent == null) {
            throw new IllegalArgumentException("No matching JavaScript file found for: " + fileName);
        }
        return jsContent;
    }
}
