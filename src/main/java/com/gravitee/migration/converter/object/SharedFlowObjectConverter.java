package com.gravitee.migration.converter.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gravitee.migration.util.policy.PolicyMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;

import static com.gravitee.migration.util.constants.CommonConstants.REQUEST;
import static com.gravitee.migration.util.constants.CommonConstants.RESPONSE;

@Component
@RequiredArgsConstructor
@Slf4j
public class SharedFlowObjectConverter {

    private final ObjectMapper objectMapper;
    private final XPath xPath;
    private final PolicyMapperUtil policyMapperUtil;

    /**
     * Creates Shared Policy Group for request and response phases. (need to be imported via the management ui)
     * Request and response shared flows are created separately, which can be later used on the needed phase.
     *
     * @param sharedFlowRootXml  The root XML document of the shared flow.
     * @param sharedFlowSteps    The steps document of the shared flow.
     * @param sharedFlowPolicies The list of policies associated with the shared flow.
     * @return A list of JSON strings representing the shared flow configurations.
     * @throws Exception if an error occurs during the creation of the shared flow configurations.
     */
    public List<String> createSharedFlow(Document sharedFlowRootXml, Document sharedFlowSteps, List<Document> sharedFlowPolicies) throws Exception {
        List<String> sharedFlows = new ArrayList<>();

        // Create and add the request shared flow configuration
        ObjectNode requestSharedFlowConfig = createSharedFlowConfig(sharedFlowRootXml, REQUEST.toUpperCase(), "-Request");
        populateSteps(requestSharedFlowConfig, sharedFlowSteps, sharedFlowPolicies, REQUEST);
        sharedFlows.add(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestSharedFlowConfig));

        // Create and add the response shared flow configuration
        ObjectNode responseSharedFlowConfig = createSharedFlowConfig(sharedFlowRootXml, RESPONSE.toUpperCase(), "-Response");
        populateSteps(responseSharedFlowConfig, sharedFlowSteps, sharedFlowPolicies, RESPONSE);
        sharedFlows.add(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseSharedFlowConfig));

        return sharedFlows;
    }

    private ObjectNode createSharedFlowConfig(Document sharedFlowRootXml, String phase, String suffix) throws Exception {
        var sharedFlowName = xPath.evaluate("/SharedFlowBundle/@name", sharedFlowRootXml);

        ObjectNode sharedFlowConfig = objectMapper.createObjectNode();
        sharedFlowConfig.put("crossId", sharedFlowName.concat(suffix));
        sharedFlowConfig.put("prerequisiteMessage", "");
        sharedFlowConfig.put("apiType", "PROXY");
        sharedFlowConfig.put("phase", phase);
        sharedFlowConfig.put("name", sharedFlowName.concat(suffix));

        return sharedFlowConfig;
    }

    private void populateSteps(ObjectNode sharedFlowConfig, Document sharedFlowSteps, List<Document> sharedFlowPolicies, String flowType) throws XPathExpressionException {
        var stepsArray = sharedFlowConfig.putArray("steps");
        var sharedFlowNodeSteps = (NodeList) xPath.evaluate("/SharedFlow/Step", sharedFlowSteps, XPathConstants.NODESET);
        policyMapperUtil.applyPoliciesToFlowNodes(sharedFlowNodeSteps, sharedFlowPolicies, stepsArray, flowType, true);
    }

}
