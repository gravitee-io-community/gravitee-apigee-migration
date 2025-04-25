package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import com.gravitee.migration.util.constants.policy.PolicyTypeConstants;
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
import java.util.Map;
import java.util.Objects;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.convertDocumentToString;
import static com.gravitee.migration.util.StringUtils.isNotNullOrEmpty;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.folder.FolderConstants.RESOURCES;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.STYLESHEET;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.XSLT;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.ASSIGN_ATTRIBUTES;

/**
 * <p>Converts XSLT policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the XSLT policy.</p>
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
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        // Extract properties
        var policyName = xPath.evaluate("/XSL/@name", apiGeePolicy);
        var outputVariable = xPath.evaluate("/XSL/OutputVariable", apiGeePolicy);
        // Extract the xslt document from the resources folder
        var xslDocument = extractCorrespondingDocument(apiGeePolicy);

        createConfigurationForXslt(condition, policyName, xslDocument, apiGeePolicy, phaseArray, phase, conditionMappings);
        registerOutputVariableInContext(condition, outputVariable, phaseArray, policyName, phase, conditionMappings);
    }

    private Document extractCorrespondingDocument(Document apiGeePolicy) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        // Extract folder we are currently located in
        String currentFolder = fileReaderService.getCurrentFolder();

        // If we are in a shared flow, we need to find the first child folder
        String resourcesPath;
        if(currentFolder.contains("\\SharedFlows\\")){
            Path childFolder = fileReaderService.findFirstChildFolder(currentFolder);
            resourcesPath = Paths.get(childFolder.toString(), RESOURCES).toString();
        }
        else {
            resourcesPath = Paths.get(currentFolder, RESOURCES).toString();

        }
        // Get the resources path
        // Extract the resource URL from the policy
        var resourceUrl = xPath.evaluate("/XSL/ResourceURL", apiGeePolicy);

        // Extract the folder name and file name from the resource URL (ex. xsl://XSLT-SuppressXML.xslt)
        var folderName = resourceUrl.substring(0, resourceUrl.indexOf(':'));
        var fileName = resourceUrl.substring(resourceUrl.indexOf("//") + 2);

        // Read the policies and extract the corresponding resource document
        var sharedFlowResources = fileReaderService.parseXmlFiles(resourcesPath, folderName);

        return fileReaderService.findDocumentByName(sharedFlowResources, fileName);
    }

    private void createConfigurationForXslt(String condition, String fileName, Document xsltDocument, Document apiGeePolicy, ArrayNode scopeArray, String scope
    , Map<String, String> conditionMappings) throws Exception {
        // Create the base scope node for the XSLT policy
        var scopeObjectNode = createBasePhaseObject(condition, fileName, PolicyTypeConstants.XSLT, scopeArray, conditionMappings);
        // Create the configuration object for the XSLT policy
        var configurationObject = scopeObjectNode.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, scope.toUpperCase());
        // Set the stylesheet for the XSLT policy
        configurationObject.put(STYLESHEET, convertDocumentToString(xsltDocument));
        // Set parameters for the XSLT policy
        createXslParameters(configurationObject, apiGeePolicy);
    }

    private void createXslParameters(ObjectNode policyObjectNode, Document apiGeePolicy) throws XPathExpressionException {
        // Extract the parameters from the Apigee policy
        NodeList parameterValues = (NodeList) xPath.evaluate("/XSL/Parameters/Parameter", apiGeePolicy, XPathConstants.NODESET);

        // Only create the parameters array if there are parameters present
        if (parameterValues != null && parameterValues.getLength() > 0) {
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

    private void registerOutputVariableInContext(String condition, String outputVariable, ArrayNode scopeArray, String policyName, String scope, Map<String, String> conditionMappings) throws XPathExpressionException {
        if (isNotNullOrEmpty(outputVariable)) {
            var scopeObject = createBasePhaseObject(condition, policyName, ASSIGN_ATTRIBUTES, scopeArray, conditionMappings);
            var configurationObject = scopeObject.putObject(CONFIGURATION);
            var attributesArray = configurationObject.putArray(ATTRIBUTES);
            var attributeObject = attributesArray.addObject();

            attributeObject.put(NAME, outputVariable);
            attributeObject.put(VALUE, Objects.equals(scope, REQUEST) ? REQUEST_CONTENT_WRAPPED : RESPONSE_CONTENT_WRAPPED);
        }
    }

}
