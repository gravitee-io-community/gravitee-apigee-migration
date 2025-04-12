package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;

/**
 * This class is responsible for converting the RaiseFault policy from the old format to the new format.
 * It implements the PolicyConverter interface and provides the logic for converting the RaiseFault policy.
 */
@Component
@RequiredArgsConstructor
public class RaiseFaultConverter implements PolicyConverter {
    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return "RaiseFault".equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String phase) throws Exception {
        var policyName = xPath.evaluate("/RaiseFault/@name", apiGeePolicy);
        var scopeObject = createBaseScopeNode(stepNode, policyName, "groovy", scopeArray);
        var status = xPath.evaluate("/RaiseFault/FaultResponse/Set/StatusCode", apiGeePolicy);
        var payloadHeader = xPath.evaluate("/RaiseFault/FaultResponse/Set/Payload/@contentType", apiGeePolicy);
        var payload = xPath.evaluate("/RaiseFault/FaultResponse/Set/Payload/text()", apiGeePolicy);

        var configurationObject = scopeObject.putObject("configuration");
        configurationObject.put("scope", "REQUEST");
        configurationObject.put("script", constructGroovyPolicy(status, payloadHeader, payload));
    }

    private String constructGroovyPolicy(String status, String payloadHeader, String payload) {
        StringBuilder script = new StringBuilder();

        script.append("import io.gravitee.policy.groovy.PolicyResult.State\n");
        script.append("result.state = State.FAILURE\n");

        if (status != null && !status.isEmpty()) {
            script.append("result.code = ").append(status).append("\n");
        }

        if (payload != null && !payload.isEmpty()) {
            script.append("result.error = '{\"error\":\"").append(payload).append("\"}'\n");
        }

        if (payloadHeader != null && !payloadHeader.isEmpty()) {
            script.append("result.contentType = '").append(payloadHeader).append("'\n");
        }

        return script.toString();
    }
}
