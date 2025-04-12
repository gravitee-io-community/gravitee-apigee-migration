package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;

import static com.gravitee.migration.enums.SignatureEnum.fromValue;
import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.VERIFY_JWT;

/*
 * Converts VerifyJWT policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the VerifyJWT policy.
 */
@Component
@RequiredArgsConstructor
public class VerifyJWTConverter implements PolicyConverter {

    private final XPath xPath;

    public boolean supports(String policyType) {
        return VERIFY_JWT.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
        var policyName = xPath.evaluate("/VerifyJWT/@name", apiGeePolicy);
        var algorithm = xPath.evaluate("/VerifyJWT/Algorithm", apiGeePolicy);
        var publicKey = xPath.evaluate("/VerifyJWT/PublicKey/JWKS/@ref", apiGeePolicy);

        var scopeObject = createBaseScopeNode(stepNode, policyName, "jwt", scopeArray);

        constructConfigurationObject(scopeObject, algorithm, publicKey, scope);
    }


    private void constructConfigurationObject(ObjectNode scopeObject, String algorithm, String publicKey, String scope) {
        var configurationObject = scopeObject.putObject("configuration");
        configurationObject.put("scope", scope);
        configurationObject.put("publicKeyResolver", "GIVEN_KEY");

        var jwksObject = configurationObject.putObject("jwksConfig");
        jwksObject.put("resolverParameter", "{#context.attributes['" + publicKey + "']}");
        jwksObject.put("signature", fromValue(algorithm));
        jwksObject.put("extractClaims", false);
        // time allowance
        // Do we need to propagate it?
        jwksObject.put("propagateAuthorizationHeader", false);

    }
}
