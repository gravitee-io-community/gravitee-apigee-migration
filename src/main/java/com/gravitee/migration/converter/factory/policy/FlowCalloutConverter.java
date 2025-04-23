package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.converter.object.PlanObjectConverter;
import com.gravitee.migration.service.filereader.impl.FileReaderServiceImpl;
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

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Folder.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.FLOW_CALLOUT;


/**
 * Converts FlowCallout policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the FlowCallout policy.
 */
@Component
@Slf4j
public class FlowCalloutConverter implements PolicyConverter {

    public final FileReaderServiceImpl fileReaderService;
    private final XPath xPath;
    @Lazy
    private final PlanObjectConverter planObjectConverter;

    @Lazy
    public FlowCalloutConverter(FileReaderServiceImpl fileReaderService, XPath xPath, PlanObjectConverter planObjectConverter) {
        this.fileReaderService = fileReaderService;
        this.xPath = xPath;
        this.planObjectConverter = planObjectConverter;
    }

    @Override
    public boolean supports(String policyType) {
        return FLOW_CALLOUT.equals(policyType);
    }

    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws XPathExpressionException {
        var sharedFlowBundleNode = (Node) xPath.evaluate("/FlowCallout/SharedFlowBundle", apiGeePolicy, XPathConstants.NODE);
        var sharedFlowBundleFolderName = sharedFlowBundleNode != null ? sharedFlowBundleNode.getTextContent() : null;

        extractSharedFlowDocuments(sharedFlowBundleFolderName, phaseArray, phase);
    }

    private void extractSharedFlowDocuments(String sharedFlowBundleFolderName, ArrayNode phaseArray, String phase) {
        if (sharedFlowBundleFolderName != null && !sharedFlowBundleFolderName.isEmpty()) {
            try {
                var sharedFlowFolder = fileReaderService.findFolderStartingWith(sharedFlowBundleFolderName);
                if (sharedFlowFolder == null) {
                    log.warn("SharedFlow folder not found: {}", sharedFlowBundleFolderName);
                    return; // Exit early if the folder is not found
                }

                var sharedFlowFolderPath = sharedFlowFolder.getAbsolutePath();
                var sharedFlowPolicies = fileReaderService.parseXmlFiles(sharedFlowFolderPath, SHARED_FLOW_BUNDLE + "/" + POLICIES);
                var sharedFlow = fileReaderService.parseXmlFiles(sharedFlowFolderPath, SHARED_FLOW_BUNDLE + "/" + SHARED_FLOWS).getFirst();

                processSharedFlowBundleSteps(sharedFlow, phaseArray, sharedFlowPolicies, phase);
                fileReaderService.setCurrentFolderToInitialState();
            } catch (Exception e) {
                log.warn("Error processing SharedFlow folder '{}': {}", sharedFlowBundleFolderName, e.getMessage());
            }
        }
    }

    private void processSharedFlowBundleSteps(Document sharedFlow, ArrayNode phaseArray, List<Document> sharedFlowPolicies, String scope) throws XPathExpressionException {
        var steps = (NodeList) xPath.evaluate("/SharedFlow/Step", sharedFlow, XPathConstants.NODESET);

        planObjectConverter.applyPoliciesToFlowNodes(steps, sharedFlowPolicies, phaseArray, scope);
    }
}
