package com.gravitee.migration.converter.factory.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RaiseFaultConverter {

    public void createRaiseFaultPolicy() {
//        ObjectNode policyNode = requestArray.addObject();
//                String condition = xPath.evaluate("Condition", requestStepNode);
//                String graviteeCondition;
//                if (nonNull(condition)) {
//                    graviteeCondition = ConditionConverter.convertRegex(condition);
//                    policyNode.put("condition", graviteeCondition);
//                }
//
//                String policyName = xPath.evaluate("/RaiseFault/@name", apiGeePolicy);
//                policyNode.put(NAME, policyName);
//                String enabledValue = xPath.evaluate("/RaiseFault/@enabled", apiGeePolicy);
//                policyNode.put("enabled", Boolean.parseBoolean(enabledValue));
//                policyNode.put("policy", "raise-fault");
//
//                ObjectNode configurationObject = policyNode.putObject(CONFIGURATION);
//                String statusCode = xPath.evaluate("/RaiseFault/FaultResponse/Set/StatusCode", apiGeePolicy);
//                configurationObject.put("status", Integer.valueOf(statusCode));
//                ArrayNode rules = configurationObject.putArray("rules");
//                ObjectNode ruleObject = rules.addObject();
//
//                ruleObject.put("input", add);
//                ObjectNode constraintsObject = ruleObject.putObject("constraint");
//                constraintsObject.put("type", "SIZE");
//                constraintsObject.put("parameters", "GREATER_THAN");
//
//                String payloadResponse = xPath.evaluate("/RaiseFault/FaultResponse/Set/Payload", apiGeePolicy);
//
//                ruleObject.put("errorMessage", payloadResponse);
//
//
    }

}
