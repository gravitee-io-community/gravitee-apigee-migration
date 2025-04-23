package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.wrapValueInContextAttributes;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.KVM;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.ASSIGN_ATTRIBUTES;

/**
 * <p>Converts KeyValueMapOperations policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the KeyValueMapOperations policy.</p>
 */
@Component
@RequiredArgsConstructor
public class KeyValueMapOperationsConverter implements PolicyConverter {

    @Value("${gravitee.dictionary.name}")
    private String dictionaryName;

    private final XPath xPath;
    private final FileReaderService fileReaderService;

    @Override
    public boolean supports(String policyType) {
        return KVM.equals(policyType);
    }

    /**
     * Converts the KeyValueMapOperations policy from Apigee to Gravitee.
     * <Parameter> tags that contain ref take values from the context, all other values are taken from the dictionary.
     *
     * @param condition     The condition to be applied to the policy.
     * @param apiGeePolicy The policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        // Extract properties
        var policyName = xPath.evaluate("/KeyValueMapOperations/@name", apiGeePolicy);
        var phaseObjectNode = createBasePhaseObject(condition, policyName, ASSIGN_ATTRIBUTES, phaseArray, conditionMappings);

        var attributesArray = createAttributesArrayInConfigurationObject(phaseObjectNode, phase);

        processGetNodes(apiGeePolicy, attributesArray);
        processPutNodes(apiGeePolicy);
    }

    private ArrayNode createAttributesArrayInConfigurationObject(ObjectNode phaseObjectNode, String phase) {
        var configurationObject = phaseObjectNode.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());

        return configurationObject.putArray(ATTRIBUTES);
    }

    private void processGetNodes(Document apiGeePolicy, ArrayNode attributesArray) throws XPathExpressionException {
        // Extract the "Get" nodes from the XML document
        var getNodes = (NodeList) xPath.evaluate("/KeyValueMapOperations/Get", apiGeePolicy, XPathConstants.NODESET);

        for (int i = 0; i < getNodes.getLength(); i++) {
            var getNode = getNodes.item(i);
            addAttribute(getNode, attributesArray);
        }
    }

    private void addAttribute(Node getNode, ArrayNode attributesArray) throws XPathExpressionException {
        // Extract the "assignTo" attribute value
        String assignToValue = xPath.evaluate("@assignTo", getNode);
        var attributeObject = createAttributeObject(attributesArray, assignToValue);

        // Extract the "ref" attribute from the Key/Parameter node
        String refValue = xPath.evaluate("Key/Parameter/@ref", getNode);

        if (isRefValuePresent(refValue)) {
            handleRefValue(attributeObject, refValue);
        } else {
            handleParameterValue(getNode, attributeObject);
        }
    }

    private ObjectNode createAttributeObject(ArrayNode attributesArray, String assignToValue) {
        // Create a new attribute object in the attributes array
        var attributeObject = attributesArray.addObject();
        attributeObject.put(NAME, assignToValue);
        return attributeObject;
    }

    private boolean isRefValuePresent(String refValue) {
        // Check if the "ref" attribute is present and not empty
        return refValue != null && !refValue.isEmpty();
    }

    private void handleRefValue(ObjectNode attributeObject, String refValue) {
        // Handle the "ref" attribute value
        if (refValue.equalsIgnoreCase(API_PROXY_NAME)) {
            attributeObject.put(VALUE, wrapValueInContextAttributes(API));
        } else {
            attributeObject.put(VALUE, wrapValueInContextAttributes(refValue));
        }
    }

    private void handleParameterValue(Node getNode, ObjectNode attributeObject) throws XPathExpressionException {
        // Handle the parameter value when "ref" is not present
        String parameterTextContent = xPath.evaluate("Key/Parameter/text()", getNode);
        // Add the parameter value to the dictionary, with a value that needs to be replaced
        fileReaderService.addValueToDictionaryMap(parameterTextContent, CHANGE_ME);
        attributeObject.put(VALUE, String.format(DICTIONARY_FORMAT_WRAPPED, dictionaryName, parameterTextContent));
    }

    private void processPutNodes(Document apiGeePolicy) throws XPathExpressionException {
        // Extract the "Put" nodes from the XML document
        var putParameters = (NodeList) xPath.evaluate("/KeyValueMapOperations/Put", apiGeePolicy, XPathConstants.NODESET);

        // If present store them to the dictionary(KVM in Apigee)
        for (int i = 0; i < putParameters.getLength(); i++) {
            var putParameter = putParameters.item(i);
            var parameterName = xPath.evaluate("Key/Parameter/text()", putParameter);
            var parameterValue = xPath.evaluate("Value", putParameter);
            fileReaderService.addValueToDictionaryMap(parameterName, parameterValue);
        }
    }
}