package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.JAVASCRIPT;

@Component
public class JavaScriptConverter implements PolicyConverter {

    @Override
    public boolean supports(String policyType) {
        return JAVASCRIPT.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray) throws XPathExpressionException {

    }
}
