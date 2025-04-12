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
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.XML_TO_JSON;

/**
 * Converts XMLToJSON policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the XMLToJSON policy.
 */
@Component
@RequiredArgsConstructor
public class XMLToJson implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return XML_TO_JSON.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws XPathExpressionException {
        var name = xPath.evaluate("/XMLToJSON/@name", apiGeePolicy);
        var scopeNode = createBaseScopeNode(stepNode, name, "xml-json", scopeArray);

        var configurationObject = scopeNode.putObject(CONFIGURATION);
        configurationObject.put("scope", scope.toUpperCase());
    }
}
