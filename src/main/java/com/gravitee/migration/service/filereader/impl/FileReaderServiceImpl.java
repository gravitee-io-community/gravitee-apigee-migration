package com.gravitee.migration.service.filereader.impl;

import com.gravitee.migration.service.filereader.FileReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileReaderServiceImpl implements FileReaderService {

    public String readAbsolutePathOfDirectory(String folderLocation, String folderName) {
        // Used if folder has whitespaces
        folderLocation = folderLocation.replace(",", " ");

        File folder = new File(folderLocation, folderName);

        return folder.getAbsolutePath();
    }

    @Override
    public List<Document> readFiles(String folderLocation, String folderName) throws ParserConfigurationException, IOException, SAXException {
        File filesFolder = getFolder(folderLocation, folderName);
        File[] files = listFiles(filesFolder);
        List<Document> policyDocuments = new ArrayList<>();

        if (files != null) {
            for (File policyFile : files) {
                policyDocuments.add(parseXmlFile(getSecureDocumentBuilder(), policyFile));
            }
        }
        return policyDocuments;
    }

    @Override
    public Map<String, String> readJavaScriptFiles(String folderLocation, String folderName) throws IOException {
        File jsFilesFolder = getFolder(folderLocation, folderName);
        File[] jsFiles = listJavaScriptFiles(jsFilesFolder);
        Map<String, String> jsFileContents = new HashMap<>();

        if (jsFiles != null) {
            for (File jsFile : jsFiles) {
                String fileName = jsFile.getName();
                String fileContent = new String(java.nio.file.Files.readAllBytes(jsFile.toPath()));
                jsFileContents.put(fileName, fileContent);
            }
        }
        return jsFileContents;
    }

    @Override
    public File findFolderStartingWith(String sharedFlowsFolder, String folderNamePrefix) {
        File sharedFlowsDirectory = new File(sharedFlowsFolder).getParentFile().getParentFile();

        if (sharedFlowsDirectory.exists() && sharedFlowsDirectory.isDirectory()) {
            // List all subdirectories and find the first one that starts with the prefix
            File[] matchingFolders = sharedFlowsDirectory.listFiles((dir, name) -> name.startsWith(folderNamePrefix) && new File(dir, name).isDirectory());
            if (matchingFolders != null && matchingFolders.length > 0) {
                return matchingFolders[0];
            }
        }

        // If no matching folder is found, throw an exception
        throw new IllegalArgumentException("No folder found starting with: " + folderNamePrefix);
    }

    @Override
    public Document findDocumentByName(List<Document> documents, String name) {
        for (Document document : documents) {
            if (document.getDocumentURI().endsWith(name)) {
                return document;
            }
        }
        return null;
    }

    /**
     * Creates file object for the specified folder location and folder name, if folder name is null it keeps the root folder.
     *
     * @param folderLocation The root folder location to search for the XML file.
     * @return The found XML file.
     * @throws IllegalArgumentException if no XML file is found in the specified folder.
     */
    private File getFolder(String folderLocation, String folderName) {
        if (folderName == null || folderName.isEmpty()) {
            return new File(folderLocation);
        } else {
            return new File(folderLocation, folderName);
        }
    }

    /**
     * Lists all files in the specified folder.
     *
     * @param folder The folder to search for the files.
     * @return An array of the files found in the folder.
     */
    private File[] listFiles(File folder) {
        return folder.listFiles((dir, name) ->
                name.endsWith(".xml") || name.endsWith(".xsl") || name.endsWith(".xslt")
        );
    }

    /**
     * Parses the specified XML file and returns a Document object.
     *
     * @param builder The DocumentBuilder instance to use for parsing.
     * @param xmlFile The XML file to parse.
     * @return The parsed Document object.
     */
    private Document parseXmlFile(DocumentBuilder builder, File xmlFile) throws IOException, SAXException {
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();
        return document;
    }

    /**
     * Creates and returns a secure DocumentBuilder instance configured to mitigate certain
     * XML external entity (XXE) attacks and other XML-related security vulnerabilities.
     * The returned DocumentBuilder has the following features configured:
     * - Disabling loading of external DTDs
     * - Disabling external general entities
     * - Disabling external parameter entities
     * - Enabling secure processing
     *
     * @return a configured, secure DocumentBuilder instance
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the configuration requested
     */
    private DocumentBuilder getSecureDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

        return factory.newDocumentBuilder();
    }

    /**
     * Lists all JavaScript files in the specified folder.
     *
     * @param folder The folder to search for JavaScript files.
     * @return An array of JavaScript files found in the folder.
     */
    private File[] listJavaScriptFiles(File folder) {
        return folder.listFiles((dir, name) -> name.endsWith(".js"));
    }
}
