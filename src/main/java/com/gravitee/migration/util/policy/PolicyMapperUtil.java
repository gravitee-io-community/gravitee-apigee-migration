package com.gravitee.migration.util.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.policy.AdvancedPolicyConverter;
import com.gravitee.migration.converter.policy.PolicyConverter;
import com.gravitee.migration.converter.policy.impl.SharedFlowConverter;
import com.gravitee.migration.infrastructure.configuration.GraviteeELTranslator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gravitee.migration.util.constants.CommonConstants.RESPONSE;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.*;

/**
 * Utility class for mapping policies from Apigee to Gravitee.
 * This class provides methods to apply policies to flow nodes and convert them to the appropriate format.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyMapperUtil {

    private final XPath xPath;
    private final SharedFlowConverter sharedFlowConverter;
    private final List<PolicyConverter> policyConverters;
    private final List<AdvancedPolicyConverter> advancedPolicyConverters;
    private final GraviteeELTranslator graviteeELTranslator;
    // used for collecting policies from the apiproxy and shared flow that can be later set in the api definition
    @Getter
    private final Set<Document> cachePolicies = new HashSet<>();
    @Getter
    private final Set<String> collectedPolicies = new HashSet<>();

    /**
     * Applies policies to flow nodes based on the step nodes and Apigee policies.
     *
     * @param stepNodes             The list of step nodes to which policies will be applied.
     * @param apiGeePolicies        The list of Apigee policies to be converted and applied.
     * @param objectNodes           The array node where the converted policies will be added.
     * @param scope                 The scope of the policies (e.g., request, response).
     * @param isSharedFlow          Indicates if the flow is a shared flow.
     * @param currentFolderLocation The current folder location for file reading. (used for extraction of js or xslt policies)
     * @throws XPathExpressionException if an error occurs during XPath evaluation.
     */
    public void applyPoliciesToFlowNodes(NodeList stepNodes, List<Document> apiGeePolicies, ArrayNode objectNodes, String scope, boolean isSharedFlow, String currentFolderLocation) throws XPathExpressionException {
        graviteeELTranslator.loadConditionMappings();

        if (stepNodes != null && stepNodes.getLength() > 0) {

            for (int j = 0; j < stepNodes.getLength(); j++) {
                var stepNode = stepNodes.item(j);
                findAndApplyMatchingPolicy(stepNode, apiGeePolicies, objectNodes, scope, isSharedFlow, currentFolderLocation);
            }
        }
    }

    /**
     * Finds and applies the matching policy to the given step node.
     *
     * @param stepNode       The step node to which the policy will be applied.
     * @param apiGeePolicies The list of Apigee policies to search for a match.
     * @param phaseArray     The array node where the converted policies will be added (e.g, request, response, or scope(for shared flow)).
     * @param phase          The phase of the policy (e.g., request, response).
     * @param isSharedFlow   Indicates if the flow is a shared flow.
     * @throws XPathExpressionException if an error occurs during XPath evaluation.
     */
    public void findAndApplyMatchingPolicy(Node stepNode, List<Document> apiGeePolicies, ArrayNode phaseArray, String phase, boolean isSharedFlow, String currentFolderLocation) throws XPathExpressionException {
        // Extract the policy name from the step node
        var stepName = xPath.evaluate("Name", stepNode);

        for (Document apiGeePolicy : apiGeePolicies) {
            // Extract the display name of the policy
            var displayName = xPath.evaluate("/*/@name", apiGeePolicy);
            // Check if the step name matches the display name of the policy
            if (stepName.equals(displayName)) {
                applyPolicyConverter(stepNode, apiGeePolicy, phaseArray, phase, isSharedFlow, currentFolderLocation);
            }
        }
    }

    private void applyPolicyConverter(Node stepNode, Document apiGeePolicy, ArrayNode phaseArray, String phase, boolean isSharedFlow, String currentFolderLocation) throws XPathExpressionException {
        // Extract the root element name of the policy
        var rootElement = (Node) xPath.evaluate("/*", apiGeePolicy, XPathConstants.NODE);
        var policyType = rootElement.getNodeName();

        String condition = xPath.evaluate("Condition", stepNode);

        collectJwtOrApiKeyPolicies(policyType);
        collectCachePolicies(policyType, apiGeePolicy);

        processPolicy(policyType, isSharedFlow, condition, apiGeePolicy, phaseArray, phase, currentFolderLocation);
    }

    private void processPolicy(String policyType, boolean isSharedFlow, String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, String currentFolderLocation) throws XPathExpressionException {
        // Used for converting the FlowCallout of the apiproxy xml only (references the shared policy group in gravitee)
        if (isFlowCalloutPolicy(policyType, isSharedFlow)) {
            sharedFlowConverter.convert(condition, apiGeePolicy, phaseArray, phase, graviteeELTranslator.getConditionMappings());
        } else if (isUnavailableInResponsePhase(policyType, phase)) {
            // Policies not available in the response phase are skipped
        } else {
            applyConverter(policyType, condition, apiGeePolicy, phaseArray, phase, currentFolderLocation);
        }
    }

    private boolean isFlowCalloutPolicy(String policyType, boolean isSharedFlow) {
        return FLOW_CALLOUT.equals(policyType) && !isSharedFlow;
    }

    private boolean isUnavailableInResponsePhase(String policyType, String phase) {
        return (VERIFY_API_KEY.equals(policyType) || VERIFY_JWT.equals(policyType) || SPIKE_ARREST.equals(policyType)) && RESPONSE.equals(phase);
    }

    private void collectJwtOrApiKeyPolicies(String policyType) {
        if (VERIFY_JWT.equals(policyType) || VERIFY_API_KEY.equals(policyType)) {
            collectedPolicies.add(policyType);
        }
    }

    private void collectCachePolicies(String policyType, Document apiGeePolicy) {
        if (LOOKUP_CACHE.equals(policyType) || POPULATE_CACHE.equalsIgnoreCase(policyType)) {
            cachePolicies.add(apiGeePolicy);
        }
    }

    private void applyConverter(String policyType, String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, String currentFolderLocation) {
        try {
            // Map that contains the condition mappings from apiGee to Gravitee
            Map<String, String> conditionMappings = graviteeELTranslator.getConditionMappings();

            // Find the converter that supports the given policy type
            var converter = getConverter(policyType);

            // Convert the policy using the correct converter
            if (converter != null) {
                converter.convert(condition, apiGeePolicy, phaseArray, phase, conditionMappings);
            } else {
                // Used for converting policies that require the current folder location (e.g., JavaScript, XSLT, FlowCallout)
                var advancedPolicyConverter = getAdvancedPolicyConverter(policyType);
                advancedPolicyConverter.convert(condition, apiGeePolicy, phaseArray, phase, conditionMappings, currentFolderLocation);
            }
        } catch (Exception e) {
            // Log the error if the converter fails
            log.warn(e.getMessage());
        }
    }

    private PolicyConverter getConverter(String policyType) {
        return policyConverters.stream()
                .filter(converter -> converter.supports(policyType))
                .findFirst()
                .orElse(null);
    }

    private AdvancedPolicyConverter getAdvancedPolicyConverter(String policyType) {
        return advancedPolicyConverters.stream()
                .filter(converter -> converter.supports(policyType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No converter found for policy type: " + policyType));
    }
}
