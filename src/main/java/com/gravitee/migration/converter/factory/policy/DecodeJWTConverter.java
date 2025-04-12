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
import static com.gravitee.migration.util.constants.GroovyConstants.DECODE_JWT;

@Component
@RequiredArgsConstructor
public class DecodeJWTConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return GraviteeCliConstants.Policy.DECODE_JWT.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws Exception {
        var policyName = xPath.evaluate("/DecodeJWT/@name", apiGeePolicy);
        var scopeObject = createBaseScopeNode(stepNode, policyName, "groovy", scopeArray);

        var configurationObject = scopeObject.putObject("configuration");
        configurationObject.put("script", DECODE_JWT);
    }
}
