package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.CONFIGURATION;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.JSON_TO_XML;

/**
 * Converts JSONToXML policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the JSONToXML policy.
 */
@Component
@RequiredArgsConstructor
public class JsonToXmlConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return JSON_TO_XML.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws XPathExpressionException {
        var name = xPath.evaluate("/JSONToXML/@name", apiGeePolicy);

        var phaseObjectNode = createBaseScopeNode(stepNode, name, "json-xml", scopeArray);

        var configurationObject = phaseObjectNode.putObject(CONFIGURATION);
        var rootJsonElement = xPath.evaluate("/JSONToXML/Options/ObjectRootElementName", apiGeePolicy);
        configurationObject.put("rootElement", rootJsonElement);
    }


}
