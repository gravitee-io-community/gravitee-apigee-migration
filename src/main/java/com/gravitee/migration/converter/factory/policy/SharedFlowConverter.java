package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.object.PlanObjectConstants.SHARED_POLICY_GROUP_ID;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.SHARED_POLICY_GROUP;

/**
 * References the Shared Policy Group flow
 */
@Component
@RequiredArgsConstructor
public class SharedFlowConverter {

    private final XPath xPath;

    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        var flowCalloutName = xPath.evaluate("/FlowCallout/SharedFlowBundle", apiGeePolicy);
        var policyName = xPath.evaluate("/FlowCallout/@name", apiGeePolicy);

        // used to reference the correct shared policy group, depending on which phase we are currently located in
        var extendedPolicyName = phase.equalsIgnoreCase(REQUEST) ? flowCalloutName.concat("-Request") : flowCalloutName.concat("-Response");

        var phaseArrayObject = createBasePhaseObject(condition, policyName, SHARED_POLICY_GROUP, phaseArray, conditionMappings);
        phaseArrayObject.put(DESCRIPTION, extendedPolicyName);

        var configuration = phaseArrayObject.putObject(CONFIGURATION);
        configuration.put(SHARED_POLICY_GROUP_ID, extendedPolicyName);
    }
}
