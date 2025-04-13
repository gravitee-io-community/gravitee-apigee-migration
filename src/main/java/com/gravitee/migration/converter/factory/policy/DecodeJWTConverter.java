package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.util.constants.GraviteeCliConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.GraviteeCliUtils.createGroovyConfiguration;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.GROOVY;
import static com.gravitee.migration.util.constants.GroovyConstants.DECODE_JWT_GROOVY;

/**
 * Converts DecodeJWT policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the DecodeJWT policy.
 */
@Component
@RequiredArgsConstructor
public class DecodeJWTConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return GraviteeCliConstants.Policy.DECODE_JWT.equals(policyType);
    }

    /**
     * Extracts claims from the DecodeJWT policy and stores them in the context attributes.
     *
     * @param stepNode     The XML node representing the DecodeJWT policy.
     * @param apiGeePolicy The APIgee policy document.
     * @param scopeArray   The array node to which the converted policy will be added.
     * @param scope        The scope of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
        var policyName = xPath.evaluate("/DecodeJWT/@name", apiGeePolicy);
        var scopeObject = createBaseScopeNode(stepNode, policyName, GROOVY, scopeArray);

        createGroovyConfiguration(DECODE_JWT_GROOVY, scope, scopeObject);
    }
}
