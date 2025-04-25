package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.util.constants.policy.PolicyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.GraviteeCliUtils.createGroovyConfiguration;
import static com.gravitee.migration.util.StringUtils.readGroovyPolicy;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.GROOVY;

/**
 * <p>Converts DecodeJWT policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the DecodeJWT policy.</p>
 */
@Component
@RequiredArgsConstructor
public class DecodeJWTConverter implements PolicyConverter {

    @Value("${groovy.decode}")
    private String decodeJwtGroovyFileLocation;

    private final XPath xPath;

    private static final String WARNING = """
            ##############################################################
            #                      SECURITY WARNING                      #
            ##############################################################
            - You are using the DecodeJWT policy, which is a Groovy script.
            - In order for the policy to work the following security configurations need to be added in the groovy sandbox
            - class java.util.regex.Matcher
            """;

    @Override
    public boolean supports(String policyType) {
        return PolicyConstants.DECODE_JWT.equals(policyType);
    }

    /**
     * Extracts claims from the Bearer token and stores them in the context attributes.
     *
     * @param condition    The condition to be applied to the policy.
     * @param apiGeePolicy The Apigee policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.q., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        System.out.println(WARNING);
        var policyName = xPath.evaluate("/DecodeJWT/@name", apiGeePolicy);

        var phaseObject = createBasePhaseObject(condition, policyName, GROOVY, phaseArray, conditionMappings);

        // Read the Groovy script from the text file
        var policyString = readGroovyPolicy(decodeJwtGroovyFileLocation);

        // Create configuration object and set properties
        createGroovyConfiguration(policyString, phase, phaseObject);
    }
}
