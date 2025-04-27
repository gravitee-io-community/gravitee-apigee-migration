package com.gravitee.migration.service.migration.impl;

import com.gravitee.migration.converter.ApiGeeToGraviteeConverter;
import com.gravitee.migration.converter.object.SharedFlowObjectConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import com.gravitee.migration.service.filewriter.FileWriterService;
import com.gravitee.migration.service.migration.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.gravitee.migration.util.constants.folder.FolderConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationServiceImpl implements MigrationService {

    @Value("${gravitee.dictionary.output}")
    private String dictionaryOutputCsv;

    private final ApiGeeToGraviteeConverter apiGeeToGraviteeConverter;
    private final FileReaderService fileReaderService;
    private final FileWriterService fileWriterService;
    private final SharedFlowObjectConverter sharedFlowObjectConverter;
    private final XPath xPath;

    @Override
    public String start(String folderLocationString) {
        try {
            // Process the shared flows first
            processSharedFlowsFolder(folderLocationString);
            // Process the APIProxy
            processApiProxyFolder(folderLocationString);

            return "Migration completed.";
        } catch (Exception e) {
            return "Migration failed. " + e.getMessage();
        }
    }

    private void processSharedFlowsFolder(String inputFolderLocation) throws Exception {
        // Get the parent of the root folder
        File rootFolderFile = new File(inputFolderLocation);
        // Get the parent folder of the root folder
        File parentFolder = rootFolderFile.getParentFile();

        // Read the SharedFlows folder
        String sharedFlowsFolderString = fileReaderService.readAbsolutePathOfDirectory(parentFolder.getAbsolutePath(), "SharedFlows");
        File sharedFlowsFolder = new File(sharedFlowsFolderString);

        if (!sharedFlowsFolder.exists() || !sharedFlowsFolder.isDirectory()) {
            throw new IllegalArgumentException("SharedFlows folder does not exist, please create the folder and place all shared flows inside the folder");
        }

        processSharedFlows(sharedFlowsFolder);
    }

    private void processApiProxyFolder(String inputFolderLocation) throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        String apiProxyFolder = fileReaderService.readAbsolutePathOfDirectory(inputFolderLocation, "apiproxy");
        startApiProxyMigration(apiProxyFolder);
    }

    private void processSharedFlows(File sharedFlowsFolder) throws Exception {
        File[] sharedFlowFolders = sharedFlowsFolder.listFiles(File::isDirectory);

        if (sharedFlowFolders == null || sharedFlowFolders.length == 0) {
            System.out.println("No shared flows found in: " + sharedFlowsFolder.getAbsolutePath());
            return;
        }

        System.out.println("Starting shared flows migration");

        String jarDir = Paths.get("").toAbsolutePath().toString();
        File sharedFlowExportFile = fileWriterService.createExportDirectory(jarDir);

        for (File sharedFlowFolder : sharedFlowFolders) {
            startSharedFlowMigration(sharedFlowFolder.getAbsolutePath(), sharedFlowExportFile);
        }
        fileWriterService.generateAndSaveMasterScript(sharedFlowExportFile);
    }

    private void startApiProxyMigration(String apiGeeFolderLocation) throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
        System.out.println("Loading apiproxy files...");

        // Extract the xml under /apiproxy that contains information about the API
        Document apiProxyRootXml = fileReaderService.readFiles(apiGeeFolderLocation, null).getFirst();
        // Extract the proxy xml that contains the proxy steps (located in /apiproxy/proxies)
        Document proxyDocument = fileReaderService.readFiles(apiGeeFolderLocation, PROXIES).getFirst();
        // Extract the policies (located in /apiproxy/policies)
        List<Document> policyDocuments = fileReaderService.readFiles(apiGeeFolderLocation, POLICIES);
        // Extract the targets (located in /apiproxy/targets)
        List<Document> targetDocuments = fileReaderService.readFiles(apiGeeFolderLocation, TARGETS);

        validateApiGeeDocument(apiProxyRootXml);

        // Write the values that need to be entered in the dictionary in gravitee to a csv file
        fileWriterService.dictionaryMapToCsv(dictionaryOutputCsv);

        // Start the conversion process and save the output to a json file
        String graviteeApiConfig = apiGeeToGraviteeConverter.apiGeeToGraviteeConverter(apiProxyRootXml, proxyDocument, policyDocuments, targetDocuments, apiGeeFolderLocation);
        fileWriterService.saveJsonToFile(graviteeApiConfig);
    }

    private void startSharedFlowMigration(String sharedFolderLocation, File sharedFlowExportFile) throws Exception {
        System.out.println("Loading Shared Flow files...");

        // Finds the shared flow folder and returns the absolute path
        String sharedFlowBundleAbsolutePath = fileReaderService.readAbsolutePathOfDirectory(sharedFolderLocation, SHARED_FLOW_BUNDLE);

        // Validate the shared flow directory
        if (!validateSharedFlowDirectory(sharedFlowBundleAbsolutePath)) {
            return; // Skip processing this shared flow
        }

        // Extract the xml under /sharedflowbundle that contains information about the shared flow
        Document sharedFlowRootXml = fileReaderService.readFiles(sharedFlowBundleAbsolutePath, null).getFirst();
        // Extract the shared flow bundle xml that contains all the steps (located in /sharedflowbundle/sharedflows)
        Document sharedFlowStepsXml = fileReaderService.readFiles(sharedFlowBundleAbsolutePath, SHARED_FLOWS).getFirst();
        // Extract the policies (located in /sharedflowbundle/policies)
        List<Document> sharedFlowPolicies = fileReaderService.readFiles(sharedFlowBundleAbsolutePath, POLICIES);

        fileWriterService.dictionaryMapToCsv(dictionaryOutputCsv);

        // Construct the sharedflow
        List<String> sharedFlows = sharedFlowObjectConverter.createSharedFlow(sharedFlowRootXml, sharedFlowStepsXml, sharedFlowPolicies, sharedFlowBundleAbsolutePath);

        // Extract the shared flow name
        var sharedFlowName = xPath.evaluate("/SharedFlowBundle/DisplayName", sharedFlowRootXml);
        // Save the output to a json file and create a curl command to import the shared flow into Gravitee
        fileWriterService.generateAndSaveCurlScript(sharedFlows, sharedFlowName, sharedFlowExportFile);
    }

    private boolean validateSharedFlowDirectory(String sharedFlowBundleAbsolutePath) {
        File sharedFlowBundleFile = new File(sharedFlowBundleAbsolutePath);
        if (!sharedFlowBundleFile.exists() || !sharedFlowBundleFile.isDirectory()) {
            log.warn("Skipping..., The following directory is not a shared flow: {}", sharedFlowBundleAbsolutePath);
            return false;
        }
        return true;
    }

    private void validateApiGeeDocument(Document apiGeeDocument) {
        // Check for the existence of a root element and certain tag names
        String rootElement = apiGeeDocument.getDocumentElement().getNodeName();
        if (!"APIProxy".equals(rootElement)) {
            throw new IllegalArgumentException("The provided document is not a valid ApiGee configuration.");
        }
    }
}
