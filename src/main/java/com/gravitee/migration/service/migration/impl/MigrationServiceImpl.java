package com.gravitee.migration.service.migration.impl;

import com.gravitee.migration.converter.ApiGeeToGraviteeConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import com.gravitee.migration.service.migration.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Folder.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationServiceImpl implements MigrationService {

    @Value("${gravitee.dictionary.output}")
    private String dictionaryOutputCsv;

    private final ApiGeeToGraviteeConverter apiGeeToGraviteeConverter;
    private final FileReaderService fileReaderService;

    @Override
    public String start(String apiGeeFolderLocation) {
        try {
            System.out.println("Loading files...");

            String apiProxyFolderLocation = fileReaderService.findApiProxyDirectory(apiGeeFolderLocation);

            Document apiProxyRootXml = fileReaderService.parseXmlFiles(apiProxyFolderLocation, null).getFirst();
            Document proxyDocument = fileReaderService.parseXmlFiles(apiProxyFolderLocation, PROXIES).getFirst();
            List<Document> policyDocuments = fileReaderService.parseXmlFiles(apiProxyFolderLocation, POLICIES);
            List<Document> targetDocuments = fileReaderService.parseXmlFiles(apiProxyFolderLocation, TARGETS);

            validateApiGeeDocument(apiProxyRootXml);

            String graviteeApiConfig = apiGeeToGraviteeConverter.apiGeeToGraviteeConverter(apiProxyRootXml, proxyDocument, policyDocuments, targetDocuments);
            fileReaderService.dictionaryMapToCsv(dictionaryOutputCsv);

            saveJsonToFile(graviteeApiConfig);

            return "Migration completed.";
        } catch (Exception e) {
            return "Migration failed." + e.getMessage();
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
