package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
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
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.KVM;

@Component
@RequiredArgsConstructor
public class KVMConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return KVM.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray) throws Exception {
        var mapIdentifier = xPath.evaluate("/KeyValueMapOperations/@mapIdentifier", apiGeePolicy);
        var policyName = xPath.evaluate("/KeyValueMapOperations/@name", apiGeePolicy);
        var phaseObjectNode = createBaseScopeNode(stepNode, policyName, "policy-assign-attributes", scopeArray);

        var getNodes = (NodeList) xPath.evaluate("/KeyValueMapOperations/Get", apiGeePolicy, XPathConstants.NODESET);

        var configurationObject = phaseObjectNode.putObject("configuration");
        var attributesArray = configurationObject.putArray("attributes");

        processGetNodes(getNodes, mapIdentifier, attributesArray);
    }

    private void processGetNodes(NodeList getNodes, String mapIdentifier, ArrayNode attributesArray) throws XPathExpressionException {
        for (int i = 0; i < getNodes.getLength(); i++) {
            var getNode = getNodes.item(i);
            addAttribute(getNode, mapIdentifier, attributesArray);
        }
    }

    private void addAttribute(Node getNode, String mapIdentifier, ArrayNode attributesArray) throws XPathExpressionException {
        var assignToValue = xPath.evaluate("@assignTo", getNode);

        // Get the text content of <Parameter>
        var parameterTextContent = xPath.evaluate("Key/Parameter/text()", getNode);

        var attributeObject = attributesArray.addObject();
        attributeObject.put("name", assignToValue);

        if (parameterTextContent == null || parameterTextContent.isEmpty()) {
            // If <Parameter> is empty, use the ref attribute
            var ref = xPath.evaluate("Key/Parameter/@ref", getNode);
            attributeObject.put("value", "{#dictionaries['" + mapIdentifier + "'][#context.attributes[" + ref + "]]}");
        } else {
            // If <Parameter> has text content, use it directly
            attributeObject.put("value", "{#dictionaries['" + mapIdentifier + "'][" + parameterTextContent + "]}");
        }
    }
}