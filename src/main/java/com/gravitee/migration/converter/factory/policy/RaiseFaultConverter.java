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
import static com.gravitee.migration.util.StringUtils.readFileFromClasspath;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.RAISE_FAULT;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.GROOVY;

/**
 * This class is responsible for converting the RaiseFault policy from the old format to the new format.
 * It implements the PolicyConverter interface and provides the logic for converting the RaiseFault policy.
 */
@Component
@RequiredArgsConstructor
public class RaiseFaultConverter implements PolicyConverter {

    @Value("${groovy.raise-fault}")
    private String raiseFaultGroovyFileLocation;

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return RAISE_FAULT.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws Exception {
        var policyName = xPath.evaluate("/RaiseFault/@name", apiGeePolicy);
        var scopeObject = createBaseScopeNode(stepNode, policyName, GROOVY, scopeArray);
        var status = xPath.evaluate("/RaiseFault/FaultResponse/Set/StatusCode", apiGeePolicy);
        var payloadHeader = xPath.evaluate("/RaiseFault/FaultResponse/Set/Payload/@contentType", apiGeePolicy);
        var payload = xPath.evaluate("/RaiseFault/FaultResponse/Set/Payload/text()", apiGeePolicy);

        var configurationObject = scopeObject.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());
        configurationObject.put(SCRIPT, constructGroovyPolicy(status, payloadHeader, payload));
    }

    private String constructGroovyPolicy(String status, String payloadHeader, String payload) throws Exception {
        if (status == null || status.isEmpty()) {
            status = "500"; // Default status
        }
        if (payload == null || payload.isEmpty()) {
            payload = "application/json"; // Default payload
        }

        var policyString = readFileFromClasspath(raiseFaultGroovyFileLocation);

        return String.format(policyString, status, payload, payloadHeader);
    }
}
