package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.GraviteeCliUtils.createGroovyConfiguration;
import static com.gravitee.migration.util.StringUtils.readFileFromClasspath;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.MESSAGE_VALIDATION;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.GROOVY;

/**
 * Converts MessageValidation policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the MessageValidation policy.
 */
@Component
@RequiredArgsConstructor
public class MessageValidationConverter implements PolicyConverter {

    @Value("${groovy.message-validation}")
    private String messageValidationGroovyFileLocation;

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return MESSAGE_VALIDATION.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
        var policyName = xPath.evaluate("/MessageValidation/@name", apiGeePolicy);

        var scopeObject = createBaseScopeNode(stepNode, policyName, GROOVY, scopeArray);
        var policyString = readFileFromClasspath(messageValidationGroovyFileLocation);
        createGroovyConfiguration(policyString, scope, scopeObject);
    }
}
