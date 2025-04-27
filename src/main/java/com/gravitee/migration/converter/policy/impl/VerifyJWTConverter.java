package com.gravitee.migration.converter.policy.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.policy.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;

import java.util.Map;

import static com.gravitee.migration.enums.SignatureEnum.fromValue;
import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.wrapValueInContextAttributes;
import static com.gravitee.migration.util.constants.CommonConstants.CONFIGURATION;
import static com.gravitee.migration.util.constants.CommonConstants.SCOPE;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.*;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.VERIFY_JWT;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.JWT;

/**
 * <p>Converts VerifyJWT policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the VerifyJWT policy.</p>
 */
@Component
@RequiredArgsConstructor
public class VerifyJWTConverter implements PolicyConverter {

    private final XPath xPath;

    public boolean supports(String policyType) {
        return VERIFY_JWT.equals(policyType);
    }

    /**
     * Converts the VerifyJWT policy from Apigee to Gravitee.
     *
     * @param condition The condition to be applied to the policy.
     * @param apiGeePolicy The Apigee policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        // Extract properties
        var policyName = xPath.evaluate("/VerifyJWT/@name", apiGeePolicy);
        var algorithm = xPath.evaluate("/VerifyJWT/Algorithm", apiGeePolicy);
        var publicKey = xPath.evaluate("/VerifyJWT/PublicKey/JWKS/@ref", apiGeePolicy);

        var phaseObject = createBasePhaseObject(condition, policyName, JWT, phaseArray, conditionMappings);

        constructConfigurationObject(phaseObject, algorithm, publicKey, phase);
    }

    private void constructConfigurationObject(ObjectNode phaseObject, String algorithm, String publicKey, String phase) {
        var configurationObject = phaseObject.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());
        configurationObject.put(PUBLIC_KEY_RESOLVER, GIVEN_KEY);

        var jwksObject = configurationObject.putObject(JWKS_CONFIG);
        jwksObject.put(RESOLVER_PARAMETER, wrapValueInContextAttributes(publicKey));
        jwksObject.put(SIGNATURE, fromValue(algorithm));
        jwksObject.put(EXTRACT_CLAIMS, true);
        // time allowance
        // Do we need to propagate it?
        jwksObject.put(PROPAGATE_AUTHORIZATION_HEADER, false);

    }
}
