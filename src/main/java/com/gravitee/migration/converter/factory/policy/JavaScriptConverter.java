package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
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

        createConfigurationForJavascript(stepNode, policyName, document, scopeArray);
    }

    private String extractJavaScriptContent(Document apiGeePolicy) throws XPathExpressionException, IOException {
        String resourcesPath = resolveResourcesPath();
        var resourceUrl = xPath.evaluate("/Javascript/ResourceURL", apiGeePolicy);
        var folderName = resourceUrl.substring(0, resourceUrl.indexOf(':'));
        var fileName = resourceUrl.substring(resourceUrl.indexOf("//") + 2);

        return getJavaScriptContent(resourcesPath, folderName, fileName);
    }

    private String resolveResourcesPath()  {
        String currentFolder = fileReaderService.getCurrentFolder();
        Path childFolder = fileReaderService.findFirstChildFolder(currentFolder);
        return Paths.get(childFolder.toString(), "resources").toString();
    }

    private String getJavaScriptContent(String resourcesPath, String folderName, String fileName) throws IOException {
        var sharedFlowResources = fileReaderService.parseJavaScriptFiles(resourcesPath, folderName);
        var jsContent = sharedFlowResources.get(fileName);

        if (jsContent == null) {
            throw new IllegalArgumentException("No matching JavaScript file found for: " + fileName);
        }
        return jsContent;
    }

    private void createConfigurationForJavascript(Node stepNode, String fileName, String javaScriptContent, ArrayNode scopeArray) throws Exception {
        var objectNode = createBaseScopeNode(stepNode, fileName, "javascript", scopeArray);
        var configurationObject = objectNode.putObject("configuration");

        configurationObject.put("onRequestScript", javaScriptContent);
    }
}
