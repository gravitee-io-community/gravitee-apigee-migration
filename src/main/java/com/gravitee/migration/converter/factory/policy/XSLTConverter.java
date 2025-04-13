package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import com.gravitee.migration.util.constants.GraviteeCliConstants;
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
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Folder.RESOURCES;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Plan.STYLESHEET;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.XSLT;

/**
 * This class is responsible for converting XSLT policies from Apigee to Gravitee format.
 * It implements the PolicyConverter interface and provides methods to convert XSLT policies.
 */
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
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
        var policyName = xPath.evaluate("/XSL/@name", apiGeePolicy);
        var xslDocument = extractCorrespondingDocument(apiGeePolicy);

        createConfigurationForXslt(stepNode, policyName, xslDocument, apiGeePolicy, scopeArray, scope);
    }

    private Document extractCorrespondingDocument(Document apiGeePolicy) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        // Extract folder we are currently located in
        String currentFolder = fileReaderService.getCurrentFolder();
        // Get the first child folder of the current folder(apiproxy or sharedflowbundle)
        Path childFolder = fileReaderService.findFirstChildFolder(currentFolder);
        // Get the resources path
        String resourcesPath = Paths.get(childFolder.toString(), RESOURCES).toString();
        // Extract the resource URL from the policy
        var resourceUrl = xPath.evaluate("/XSL/ResourceURL", apiGeePolicy);

        // Extract the folder name and file name from the resource URL (ex. xsl://XSLT-SuppressXML.xslt)
        var folderName = resourceUrl.substring(0, resourceUrl.indexOf(':'));
        var fileName = resourceUrl.substring(resourceUrl.indexOf("//") + 2);

        // Read the policies and extract the corresponding resource document
        var sharedFlowResources = fileReaderService.parseXmlFiles(resourcesPath, folderName);

        return fileReaderService.findDocumentByName(sharedFlowResources, fileName);
    }

    private void createConfigurationForXslt(Node stepNode, String fileName, Document xsltDocument, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
        // Create the base scope node for the XSLT policy
        var scopeObjectNode = createBaseScopeNode(stepNode, fileName, GraviteeCliConstants.PolicyType.XSLT, scopeArray);
        // Create the configuration object for the XSLT policy
        var configurationObject = scopeObjectNode.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, scope.toUpperCase());
        // Set the stylesheet for the XSLT policy
        configurationObject.put(STYLESHEET, convertDocumentToString(xsltDocument));
        // Set parameters for the XSLT policy
        createXslParameters(configurationObject, apiGeePolicy);
    }

    private void createXslParameters(ObjectNode policyObjectNode, Document apiGeePolicy) throws XPathExpressionException {
        // Extract the parameters from the APIgee policy
        NodeList parameterValues = (NodeList) xPath.evaluate("/XSL/Parameters/Parameter", apiGeePolicy, XPathConstants.NODESET);

        // Only create the parameters array if there are parameters present
        if (parameterValues.getLength() > 0) {
            var parameters = policyObjectNode.putArray(PARAMETERS);

            for (int i = 0; i < parameterValues.getLength(); i++) {
                var parameter = parameterValues.item(i);
                addParameterToArray(parameters, parameter);
            }
        }
    }

    private void addParameterToArray(ArrayNode parameters, Node parameter) throws XPathExpressionException {
        var name = xPath.evaluate("@name", parameter);
        var value = xPath.evaluate("@ref", parameter);

        if (!name.isEmpty() && !value.isEmpty()) {
            var parameterObject = parameters.addObject();
            parameterObject.put(NAME, name);
            parameterObject.put(VALUE, value);
        }
    }
}
