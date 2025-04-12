package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.VERIFY_API_KEY;

@Component
@RequiredArgsConstructor
public class VerifyAPIKeyConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return VERIFY_API_KEY.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
        var policyName = xPath.evaluate("/VerifyAPIKey/@name", apiGeePolicy);
        var apiKeyRef = xPath.evaluate("/VerifyAPIKey/APIKey/@ref", apiGeePolicy);

        // First we add the transform-header policy to add the extracted apikey to the X-Gravitee-Api-Key header
        var scopeObjectHeaders = createBaseScopeNode(stepNode, policyName, "transform-headers", scopeArray);
        var configurationObjectHeaders = scopeObjectHeaders.putObject("configuration");
        configurationObjectHeaders.put("scope", scope.toUpperCase());
        var configurationsArray =  configurationObjectHeaders.putArray("addHeaders");
        var configurationObject = configurationsArray.addObject();
        configurationObject.put("name", "X-Gravitee-Api-Key");
        configurationObject.put("value", "{#context.attributes['" + apiKeyRef + "']}");

        var scopeObject = createBaseScopeNode(stepNode, policyName, "api-key", scopeArray);

        var configurationObject2 = scopeObject.putObject("configuration");
        configurationObject2.put("propagateApiKey", false);

    }
}
