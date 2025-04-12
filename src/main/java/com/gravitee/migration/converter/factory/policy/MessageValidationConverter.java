package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GroovyConstants.MESSAGE_VALIDATION;

/*
 * Converts MessageValidation policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the MessageValidation policy.
 */
@Component
@RequiredArgsConstructor
public class MessageValidationConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return MESSAGE_VALIDATION.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws Exception {
        var policyName = xPath.evaluate("/MessageValidation/@name", apiGeePolicy);

        var scopeObject = createBaseScopeNode(stepNode, policyName, "groovy", scopeArray);
        scopeObject.put("script", MESSAGE_VALIDATION);
    }
}
