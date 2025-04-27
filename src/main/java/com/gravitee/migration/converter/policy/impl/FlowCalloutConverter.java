package com.gravitee.migration.converter.policy.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.policy.AdvancedPolicyConverter;
import com.gravitee.migration.service.filereader.impl.FileReaderServiceImpl;
import com.gravitee.migration.util.policy.PolicyMapperUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import java.util.Map;

import static com.gravitee.migration.util.constants.folder.FolderConstants.*;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.FLOW_CALLOUT;


/**
 * Converts FlowCallout policy from Apigee to Gravitee. (used for nesting shared flows withing shared flows)
 * This class implements the PolicyConverter interface and provides the logic to convert the FlowCallout policy.
 */
@Component
@Slf4j
public class FlowCalloutConverter implements AdvancedPolicyConverter {

    public final FileReaderServiceImpl fileReaderService;
    private final XPath xPath;
    @Lazy
    private final PolicyMapperUtil policyMapperUtil;

    @Lazy
    public FlowCalloutConverter(FileReaderServiceImpl fileReaderService, XPath xPath, PolicyMapperUtil policyMapperUtil) {
        this.fileReaderService = fileReaderService;
        this.xPath = xPath;
        this.policyMapperUtil = policyMapperUtil;
    }


    @Override
    public boolean supports(String policyType) {
        return FLOW_CALLOUT.equals(policyType);
    }

    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings, String currentFolderLocation) throws XPathExpressionException {
        var sharedFlowBundleNode = (Node) xPath.evaluate("/FlowCallout/SharedFlowBundle", apiGeePolicy, XPathConstants.NODE);
        var sharedFlowBundleFolderName = sharedFlowBundleNode != null ? sharedFlowBundleNode.getTextContent() : null;

        extractSharedFlowDocuments(sharedFlowBundleFolderName, phaseArray, phase, currentFolderLocation);
    }

    private void extractSharedFlowDocuments(String sharedFlowBundleFolderName, ArrayNode phaseArray, String phase, String currentFolderLocation) {
        if (sharedFlowBundleFolderName != null && !sharedFlowBundleFolderName.isEmpty()) {
            try {
                var sharedFlowFolder = fileReaderService.findFolderStartingWith(currentFolderLocation, sharedFlowBundleFolderName);
                if (sharedFlowFolder == null) {
                    log.warn("SharedFlow folder not found: {}", sharedFlowBundleFolderName);
                    return; // Exit early if the folder is not found
                }

                var sharedFlowFolderPath = sharedFlowFolder.getAbsolutePath();

                // Read the sharedflowbundle folder and extract the shared flow steps and policies
                var sharedFlowBundle = fileReaderService.readAbsolutePathOfDirectory(sharedFlowFolderPath, SHARED_FLOW_BUNDLE);
                var sharedFlowPolicies = fileReaderService.readFiles(sharedFlowBundle,  POLICIES);
                var sharedFlow = fileReaderService.readFiles(sharedFlowBundle, SHARED_FLOWS).getFirst();

                // Process the shared flow steps
                processSharedFlowBundleSteps(sharedFlow, phaseArray, sharedFlowPolicies, phase, sharedFlowBundle);
            } catch (Exception e) {
                log.warn("Error processing SharedFlow folder '{}': {}", sharedFlowBundleFolderName, e.getMessage());
            }
        }
    }

    private void processSharedFlowBundleSteps(Document sharedFlow, ArrayNode phaseArray, List<Document> sharedFlowPolicies, String scope, String sharedFlowBundle) throws XPathExpressionException {
        var steps = (NodeList) xPath.evaluate("/SharedFlow/Step", sharedFlow, XPathConstants.NODESET);

        policyMapperUtil.applyPoliciesToFlowNodes(steps, sharedFlowPolicies, phaseArray, scope, false, sharedFlowBundle);
    }
}
