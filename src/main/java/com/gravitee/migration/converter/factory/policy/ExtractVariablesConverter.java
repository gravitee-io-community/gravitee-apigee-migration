package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.util.ConditionConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.CONFIGURATION;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.NAME;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.EXTRACT_VARIABLES;
import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
public class ExtractVariablesConverter implements PolicyConverter {

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return EXTRACT_VARIABLES.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode requestArray) throws XPathExpressionException {
        // TODO: SPLIT INTO MULTIPLE METHODS AND SEE OTHER EXAMPLE IF IT IS NOT JSONPayload
        var jsonPayloadVariables = (NodeList) xPath.evaluate("/ExtractVariables/JSONPayload/Variable", apiGeePolicy, XPathConstants.NODESET);

        if (jsonPayloadVariables != null && jsonPayloadVariables.getLength() > 0) {
            var policyNode = requestArray.addObject();
            var condition = xPath.evaluate("Condition", stepNode);
            if (nonNull(condition)) {
                var graviteeCondition = ConditionConverter.convertApigeeConditionToGravitee(condition);
                policyNode.put("condition", graviteeCondition);
            }

            var policyName = xPath.evaluate("/ExtractVariables/@name", apiGeePolicy);
            policyNode.put(NAME, policyName);
            var enabledValue = xPath.evaluate("/ExtractVariables/@enabled", apiGeePolicy);
            policyNode.put("enabled", Boolean.parseBoolean(enabledValue));
            policyNode.put("policy", "policy-assign-attributes");
            var configuration = policyNode.putObject(CONFIGURATION);

            var attributes = configuration.putArray("attributes");
            var prefix = xPath.evaluate("/ExtractVariables/VariablePrefix", apiGeePolicy);

            for (int i = 0; i < jsonPayloadVariables.getLength(); i++) {
                var jsonPayloadVariable = jsonPayloadVariables.item(i);
                var attributeNode = attributes.addObject();
                var jsonVariableName = xPath.evaluate("@name", jsonPayloadVariable);
                var jsonPath = xPath.evaluate("JSONPath", jsonPayloadVariable);

                attributeNode.put("name", prefix + "." + jsonVariableName);
                attributeNode.put("value", "{#jsonPath(#request.content, '" + jsonPath + "')}");
            }
        }
    }
}

