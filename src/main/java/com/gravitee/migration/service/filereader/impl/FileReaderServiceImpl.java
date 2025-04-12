package com.gravitee.migration.service.filereader.impl;

import com.gravitee.migration.service.filereader.FileReaderService;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Folder.API_PROXY;

@Service
public class FileReaderServiceImpl implements FileReaderService {

    private String inputFolderLocation;
    private String currentFolder;
    private String apiProxyFolderLocation;

    @Override
    public String findApiProxyDirectory(String folderLocation) {
        folderLocation = folderLocation.replace(",", " ");
        File rootFolder = new File(folderLocation);

        if (rootFolder.exists() && rootFolder.isDirectory()) {
            for (File projectFolder : Objects.requireNonNull(rootFolder.listFiles(File::isDirectory))) {
                // Look for apiproxy folder within each project folder
                File possibleApiProxyFolder = new File(projectFolder, API_PROXY);

                if (possibleApiProxyFolder.exists() && possibleApiProxyFolder.isDirectory()) {
                    inputFolderLocation = rootFolder.getAbsolutePath();
                    apiProxyFolderLocation = possibleApiProxyFolder.getAbsolutePath();
                    return possibleApiProxyFolder.getAbsolutePath(); // Return the absolute path of the found apiproxy folder
                }
            }
        }
        throw new IllegalArgumentException("No apiproxy folder found in the specified location.");
    }

    @Override
    public List<Document> parseXmlFiles(String folderLocation, String folderName) throws ParserConfigurationException, IOException, SAXException {
        File xmlFilesFolder = getFolder(folderLocation, folderName);
        File[] xmlFiles = listXmlFiles(xmlFilesFolder);
        List<Document> policyDocuments = new ArrayList<>();

        if (xmlFiles != null) {
            for (File policyFile : xmlFiles) {
                policyDocuments.add(parseXmlFile(getSecureDocumentBuilder(), policyFile));
            }
        }
        return policyDocuments;
    }

    @Override
    public Map<String, String> parseJavaScriptFiles(String folderLocation, String folderName) throws IOException {
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
    public File findFolderStartingWith(String folderNamePrefix) {
        String fileInput = inputFolderLocation + "/SharedFlows";
        File parentDir = new File(fileInput);
        File[] matchingFolders = parentDir.listFiles((dir, name) -> name.startsWith(folderNamePrefix));
        if (matchingFolders == null || matchingFolders.length == 0) {
            throw new IllegalArgumentException("No folder found starting with: " + folderNamePrefix);
        }
        currentFolder = matchingFolders[0].getAbsolutePath();
        return matchingFolders[0];
    }

    @Override
    public void setCurrentFolderToInitialState() {
        this.currentFolder = apiProxyFolderLocation.replace("\\apiproxy", "");
    }

    @Override
    public String getCurrentFolder() {
        return currentFolder;
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

    public Path findFirstChildFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] childFolders = folder.listFiles(File::isDirectory);
        if (childFolders != null && childFolders.length > 0) {
            return childFolders[0].toPath();
        } else {
            throw new IllegalArgumentException("No child folders found in the specified location.");
        }
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
     * Lists all XML files in the specified folder.
     *
     * @param folder The folder to search for XML files.
     * @return An array of XML files found in the folder.
     */
    private File[] listXmlFiles(File folder) {
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
