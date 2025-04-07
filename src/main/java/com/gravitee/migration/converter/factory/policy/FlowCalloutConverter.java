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

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Folder.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.FLOW_CALLOUT;

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
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray) throws XPathExpressionException {
        var sharedFlowBundleNode = (Node) xPath.evaluate("/FlowCallout/SharedFlowBundle", apiGeePolicy, XPathConstants.NODE);
        var sharedFlowBundleFolderName = sharedFlowBundleNode != null ? sharedFlowBundleNode.getTextContent() : null;

        extractSharedFlowDocuments(sharedFlowBundleFolderName, scopeArray);
    }

    private void extractSharedFlowDocuments(String sharedFlowBundleFolderName, ArrayNode scopeArray) {
        if (sharedFlowBundleFolderName != null && !sharedFlowBundleFolderName.isEmpty()) {
            try {
                var sharedFlowFolder = fileReaderService.findFolderStartingWith(sharedFlowBundleFolderName).getAbsolutePath();

                var sharedFlowPolicies = fileReaderService.parseXmlFiles(sharedFlowFolder, SHARED_FLOW_BUNDLE + "/" + POLICIES);
                var sharedFlowResources = fileReaderService.parseXmlFiles(sharedFlowFolder, SHARED_FLOW_BUNDLE + "/" + RESOURCES);
                var sharedFlow = fileReaderService.parseXmlFiles(sharedFlowFolder, SHARED_FLOW_BUNDLE + "/" + SHARED_FLOWS).getFirst();

                processSharedFlowBundleSteps(sharedFlow, scopeArray, sharedFlowPolicies);
                fileReaderService.setCurrentFolderToInitialState();
            } catch (Exception e) {
                log.error("folder not present");
                // TODO: ASK HOW TO INFORM CLIENT IN CASE A FOLDER DOES NO EXIST
            }
        }
    }

    private void processSharedFlowBundleSteps(Document sharedFlow, ArrayNode requestArray, List<Document> sharedFlowPolicies) throws XPathExpressionException {
        var steps = (NodeList) xPath.evaluate("/SharedFlow/Step", sharedFlow, XPathConstants.NODESET);

        planObjectConverter.applyPoliciesToFlowNodes(steps, sharedFlowPolicies, requestArray);
    }
}
