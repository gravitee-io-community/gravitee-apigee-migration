package com.gravitee.migration.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.ENABLED;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.NAME;
import static java.util.Objects.nonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GraviteeCliUtils {

    public static final XPath xPath = XPathFactory.newInstance().newXPath();

    public static ObjectNode createBaseScopeNode(Node stepNode, String policyName, String policyType, ArrayNode requestArray) throws XPathExpressionException {
        ObjectNode requestNode = requestArray.addObject();

        String condition = xPath.evaluate("Condition", stepNode);
        if (nonNull(condition) && !condition.isEmpty()) {
            String graviteeCondition = convertApigeePolicyConditionToGravitee(condition);
            requestNode.put("condition", graviteeCondition);
        }
        requestNode.put(NAME, policyName);
        //! TODO: SET FALSE FOR JAVASRIPT
        boolean isEnabled = !policyType.equals("javascript");
        requestNode.put(ENABLED, isEnabled);
        requestNode.put("policy", policyType);

        return requestNode;
    }

    public static String convertApigeePolicyConditionToGravitee(String apigeeCondition) {
        if (apigeeCondition == null || apigeeCondition.trim().isEmpty()) {
            return null; // Return null if the condition is null or empty
        }

        // Replace Apigee-specific syntax with Gravitee syntax
        return apigeeCondition
                .trim() // Trim leading and trailing whitespace
                .replace("proxy.pathsuffix !MatchesPath", "!#request.path.matches") // Negated match
                .replace("proxy.pathsuffix MatchesPath", "#request.path.matches")  // Positive match
                .replaceAll("(#request\\.path\\.matches)\\s+\"([^\"]+)\"", "$1(\"$2\")") // Ensure parentheses are added correctly
                .replace("request.verb", "#request.method"); // Handle verb to method conversion
    }
}
