package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.StringUtils.convertDocumentToString;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.XSLT;

@Component
@RequiredArgsConstructor
public class XSLTConverter implements PolicyConverter {

    private final XPath xPath;
    private final FileReaderService fileReaderService;

    @Override
    public boolean supports(String policyType) {
        return XSLT.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray) throws Exception {
        var policyName = xPath.evaluate("/XSL/@name", apiGeePolicy);
        var xslDocument = extractCorrespondingDocument(apiGeePolicy);

        // construct policies
        createChangeContentPolicy(stepNode, policyName, apiGeePolicy, scopeArray);
        createConfigurationForXslt(stepNode, policyName, xslDocument, apiGeePolicy, scopeArray);
        crateAssignAttributesPolicy(stepNode, policyName, apiGeePolicy, scopeArray);
    }

    private Document extractCorrespondingDocument(Document apiGeePolicy) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        // Extract folder we are currently located in
        String currentFolder = fileReaderService.getCurrentFolder();
        // Get the first child folder of the current folder(apiproxy or sharedflowbundle)
        Path childFolder = fileReaderService.findFirstChildFolder(currentFolder);
        // Get the resources path
        String resourcesPath = Paths.get(childFolder.toString(), "resources").toString();

        // Extract the resource URL from the policy
        var resourceUrl = xPath.evaluate("/XSL/ResourceURL", apiGeePolicy);

        // Extract the folder name and file name from the resource URL
        var folderName = resourceUrl.substring(0, resourceUrl.indexOf(':'));
        var fileName = resourceUrl.substring(resourceUrl.indexOf("//") + 2);

        // Read the policies and extract the corresponding resource document
        var sharedFlowResources = fileReaderService.parseXmlFiles(resourcesPath, folderName);

        return fileReaderService.findDocumentByName(sharedFlowResources, fileName);
    }

    private void createConfigurationForXslt(Node stepNode, String fileName, Document xsltDocument, Document apiGeePolicy, ArrayNode scopeArray) throws Exception {
        var objectNode = createBaseScopeNode(stepNode, fileName, "xslt", scopeArray);
        var configurationObject = objectNode.putObject("configuration");

        configurationObject.put("stylesheet", convertDocumentToString(xsltDocument));

        createXslParameters(configurationObject, apiGeePolicy);
    }

    private void createXslParameters(ObjectNode policyObjectNode, Document apiGeePolicy) throws XPathExpressionException {
        NodeList parameterValues = (NodeList) xPath.evaluate("/XSL/Parameters/Parameter", apiGeePolicy, XPathConstants.NODESET);

        if (parameterValues.getLength() > 0) {
            var parameters = policyObjectNode.putArray("parameters");

            for (int i = 0; i < parameterValues.getLength(); i++) {
                var parameter = parameterValues.item(i);
                var name = xPath.evaluate("@name", parameter);
                var value = xPath.evaluate("@ref", parameter);

                if (!name.isEmpty() && !value.isEmpty()) {
                    var parameterObject = parameters.addObject();
                    parameterObject.put("name", name);
                    parameterObject.put("value", "{#context.attributes['" + value + "']}");
                }
            }
        }
    }

    private void createChangeContentPolicy(Node stepNode, String fileName, Document apiGeePolicy, ArrayNode scopeArray) throws XPathExpressionException {
        var source = xPath.evaluate("/XSL/Source", apiGeePolicy);
        var objectNode = createBaseScopeNode(stepNode, fileName.concat("-Assign-Content"), "policy-assign-content", scopeArray);

        var configurationObject = objectNode.putObject("configuration");
        configurationObject.put("scope", "REQUEST");
        configurationObject.put("type", "application/xml");
        configurationObject.put("body", "{#context.attributes['" + source + "']}");

    }

    private void crateAssignAttributesPolicy(Node stepNode, String fileName, Document apiGeePolicy, ArrayNode scopeArray) throws XPathExpressionException {
        var outputVariable = xPath.evaluate("/XSL/OutputVariable", apiGeePolicy);

        if (!outputVariable.isEmpty()) {
            var objectNode = createBaseScopeNode(stepNode, fileName.concat("-Assign-Attributes"), "policy-assign-attributes", scopeArray);

            var configurationObject = objectNode.putObject("configuration");
            var attributesArray = configurationObject.putArray("attributes");
            var attributeObject = attributesArray.addObject();
            attributeObject.put("name", outputVariable);
            attributeObject.put("value", "{#request.content}");
        }
    }
}
