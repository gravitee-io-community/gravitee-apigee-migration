package com.gravitee.migration.service.migration.impl;

import com.gravitee.migration.converter.ApiGeeToGraviteeConverter;
import com.gravitee.migration.converter.object.SharedFlowObjectConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import static com.gravitee.migration.util.constants.folder.FolderConstants.*;
import static com.gravitee.migration.util.constants.groovy.GroovyConstants.CURL_COMMAND_TEMPLATE;
import static com.gravitee.migration.util.constants.groovy.GroovyConstants.MASTER_SCRIPT;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationServiceImpl implements MigrationService {

    @Value("${gravitee.dictionary.output}")
    private String dictionaryOutputCsv;

    private final ApiGeeToGraviteeConverter apiGeeToGraviteeConverter;
    private final FileReaderService fileReaderService;
    private final SharedFlowObjectConverter sharedFlowObjectConverter;
    private final XPath xPath;

    @Override
    public String start(String folderLocationString) {
        try {
            File rootFolder = validateRootFolder(folderLocationString);

            processSharedFlowsFolder(rootFolder);
            processApiProxyFolder(rootFolder);

            return "Migration completed.";
        } catch (Exception e) {
            return "Migration failed. " + e.getMessage();
        }
    }

    private File validateRootFolder(String folderLocationString) {
        File rootFolder = new File(folderLocationString);

        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            System.out.println("Invalid folder location: " + folderLocationString);
        }

        return rootFolder;
    }

    private void processSharedFlowsFolder(File rootFolder) throws Exception {
        File parentFolder = rootFolder.getParentFile();
        File sharedFlowsFolder = new File(parentFolder, "SharedFlows");

        if (sharedFlowsFolder.exists() && sharedFlowsFolder.isDirectory()) {
            processSharedFlows(sharedFlowsFolder);
        } else {
            System.out.println("SharedFlows folder not found.");
        }
    }

    private void processApiProxyFolder(File rootFolder) throws XPathExpressionException, IOException, ParserConfigurationException, SAXException {
        File apiProxyFolder = new File(rootFolder.getAbsolutePath(), API_PROXY);

        if (apiProxyFolder.exists() && apiProxyFolder.isDirectory()) {
            System.out.println("Processing API Proxy in: " + apiProxyFolder.getAbsolutePath());
            startApiProxyMigration(apiProxyFolder.getAbsolutePath());
        } else {
            System.out.println("apiproxy folder not found.");
        }
    }

    private void processSharedFlows(File sharedFlowsFolder) throws Exception {
        File[] sharedFlowFolders = sharedFlowsFolder.listFiles(File::isDirectory);

        if (sharedFlowFolders == null || sharedFlowFolders.length == 0) {
            System.out.println("No shared flows found in: " + sharedFlowsFolder.getAbsolutePath());
            return;
        }

        System.out.println("Starting shared flows migration");

        String jarDir = Paths.get("").toAbsolutePath().toString();
        File sharedFlowExportFile = createExportDirectory(jarDir);

        for (File sharedFlowFolder : sharedFlowFolders) {
            startSharedFlowMigration(sharedFlowFolder.getAbsolutePath(), sharedFlowExportFile);
        }
        generateMasterScript(sharedFlowExportFile);
    }

    private void startApiProxyMigration(String apiGeeFolderLocation) throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
        System.out.println("Loading apiproxy files...");

        // Finds the apiproxy folder and returns the absolute path
        String apiProxyFolderLocation = fileReaderService.findDirectory(apiGeeFolderLocation, null); //apiproxy was here

        // Extract the xml under /apiproxy that contains information about the API
        Document apiProxyRootXml = fileReaderService.parseXmlFiles(apiProxyFolderLocation, null).getFirst();
        // Extract the proxy xml that contains the proxy steps (located in /apiproxy/proxies)
        Document proxyDocument = fileReaderService.parseXmlFiles(apiProxyFolderLocation, PROXIES).getFirst();
        // Extract the policies (located in /apiproxy/policies)
        List<Document> policyDocuments = fileReaderService.parseXmlFiles(apiProxyFolderLocation, POLICIES);
        // Extract the targets (located in /apiproxy/targets)
        List<Document> targetDocuments = fileReaderService.parseXmlFiles(apiProxyFolderLocation, TARGETS);

        validateApiGeeDocument(apiProxyRootXml);

        fileReaderService.dictionaryMapToCsv(dictionaryOutputCsv);

        String graviteeApiConfig = apiGeeToGraviteeConverter.apiGeeToGraviteeConverter(apiProxyRootXml, proxyDocument, policyDocuments, targetDocuments);
        saveJsonToFile(graviteeApiConfig);
    }

    private void startSharedFlowMigration(String sharedFolderLocation, File sharedFlowExportFile) throws Exception {
        System.out.println("Loading Shared Flow files...");

        // Finds the shared flow folder and returns the absolute path
        String sharedFlowBundleAbsolutePath = fileReaderService.findDirectory(sharedFolderLocation, SHARED_FLOW_BUNDLE);
        // Extract the xml under /sharedflowbundle that contains information about the shared flow
        Document sharedFlowRootXml = fileReaderService.parseXmlFiles(sharedFlowBundleAbsolutePath, null).getFirst();
        // Extract the shared flow bundle xml that contains all the steps (located in /sharedflowbundle/sharedflows)
        Document sharedFlowStepsXml = fileReaderService.parseXmlFiles(sharedFlowBundleAbsolutePath, SHARED_FLOWS).getFirst();
        // Extract the policies (located in /sharedflowbundle/policies)
        List<Document> sharedFlowPolicies = fileReaderService.parseXmlFiles(sharedFlowBundleAbsolutePath, POLICIES);

        fileReaderService.dictionaryMapToCsv(dictionaryOutputCsv);

        // Construct the sharedflow
        List<String> sharedFlows = sharedFlowObjectConverter.createSharedFlow(sharedFlowRootXml, sharedFlowStepsXml, sharedFlowPolicies);

        // Extract the shared flow name
        var sharedFlowName = xPath.evaluate("/SharedFlowBundle/DisplayName", sharedFlowRootXml);
        // Save the output to a json file and create a curl command to import the shared flow into Gravitee
        generateCurlScript(sharedFlows, sharedFlowName, sharedFlowExportFile);
    }

    private void generateMasterScript(File sharedFlowDir) throws IOException {
        if (!sharedFlowDir.exists() || !sharedFlowDir.isDirectory()) {
            throw new IllegalArgumentException("The specified directory does not exist or is not a directory: " + sharedFlowDir.getAbsolutePath());
        }

        File masterScript = new File(sharedFlowDir, "execute_all_scripts.bat");
        try (FileWriter writer = new FileWriter(masterScript)) {
            // Replace placeholders in the MASTER_SCRIPT constant

            // Write the script content to the file
            writer.write(MASTER_SCRIPT);
        }
        System.out.println("Master script created at: " + masterScript.getAbsolutePath());
    }

    private void generateCurlScript(List<String> graviteeJson, String sharedFlowName, File sharedFlowExportFile) throws IOException {
        validateJsonList(graviteeJson);

        File sharedFlowDir = createDirectories(sharedFlowName, sharedFlowExportFile);

        String[] suffixes = {"-request", "-response"};
        for (int i = 0; i < graviteeJson.size(); i++) {
            if (i >= suffixes.length) {
                throw new IllegalArgumentException("The JSON list contains more elements than expected suffixes.");
            }

            String jsonFileName = String.format("%s%s.json", sharedFlowName, suffixes[i]);
            saveJsonFile(sharedFlowDir, jsonFileName, graviteeJson.get(i));

            String batchFileName = String.format("%s%s.bat", sharedFlowName, suffixes[i]);
            saveBatchScript(sharedFlowDir, batchFileName, jsonFileName);

        }
        System.out.println("Successfully migrated shared flow: " + sharedFlowName);
    }

    private void validateJsonList(List<String> graviteeJson) {
        if (graviteeJson == null || graviteeJson.isEmpty()) {
            throw new IllegalArgumentException("The JSON list must contain at least one element.");
        }
    }

    private File createDirectories(String sharedFlowName, File exportDir) throws IOException {

        File sharedFlowDir = new File(exportDir, sharedFlowName);
        if (!sharedFlowDir.exists() && !sharedFlowDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + sharedFlowDir.getAbsolutePath());
        }

        return sharedFlowDir;
    }

    private File createExportDirectory(String jarDir) throws IOException {
        File exportDir = new File(jarDir, "SharedFlowsExport");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + exportDir.getAbsolutePath());
        }
        return exportDir;
    }


    private void saveJsonFile(File sharedFlowDir, String jsonFileName, String jsonContent) throws IOException {
        File jsonFile = new File(sharedFlowDir, jsonFileName);
        try (OutputStream os = new FileOutputStream(jsonFile)) {
            os.write(jsonContent.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void saveBatchScript(File sharedFlowDir, String batchFileName, String jsonFileName) throws IOException {
        File batchFile = new File(sharedFlowDir, batchFileName);

        String batchCommand = String.format(CURL_COMMAND_TEMPLATE, jsonFileName);

        try (OutputStream os = new FileOutputStream(batchFile)) {
            os.write(batchCommand.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void validateApiGeeDocument(Document apiGeeDocument) {
        // Check for the existence of a root element and certain tag names
        String rootElement = apiGeeDocument.getDocumentElement().getNodeName();
        if (!"APIProxy".equals(rootElement)) {
            throw new IllegalArgumentException("The provided document is not a valid ApiGee configuration.");
        }
        // Add further validation logic as needed (e.g., check key tags, attributes, etc.)
    }

    private void saveJsonToFile(String graviteeJson) throws IOException {
        // Write the JSON string to the specified output file.
        String jarDir = Paths.get("").toAbsolutePath().toString();
        File outputFile = new File(jarDir, "gravitee_api_output.json");
        try (OutputStream os = new FileOutputStream(outputFile)) {
            os.write(graviteeJson.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("Gravitee JSON configuration saved to: " + outputFile.getAbsolutePath());
    }
}
