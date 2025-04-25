package com.gravitee.migration.service.filereader.impl;

import com.gravitee.migration.service.filereader.FileReaderService;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileReaderServiceImpl implements FileReaderService {

    // Shifting between folders is done in order to know what folder we are currently located in when we are performing JS or XSLT conversion
    private String inputFolderLocation;
    private String currentFolder;
    private String folderLocation;
    private final Map<String, String> dictionaryMap = new HashMap<>();

    @Override
    public String findDirectory(String folderLocation, String folderName) {
        folderLocation = folderLocation.replace(",", " ");
        File rootFolder = new File(folderLocation);

        File possibleApiProxyFolder;
        if (folderName == null || folderName.isEmpty()) {
            possibleApiProxyFolder = rootFolder;
        } else {
            possibleApiProxyFolder = new File(rootFolder, folderName);
        }

        if (possibleApiProxyFolder.exists() && possibleApiProxyFolder.isDirectory()) {
            inputFolderLocation = rootFolder.getAbsolutePath();
            this.folderLocation = possibleApiProxyFolder.getAbsolutePath();
            currentFolder = rootFolder.getAbsolutePath();
            return possibleApiProxyFolder.getAbsolutePath();
        }

        throw new IllegalArgumentException("No folder found in the specified location.");
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
        // Get the parent of inputFolderLocation
        File inputFolder = new File(inputFolderLocation);
        File parentDir = inputFolder.getParentFile();

        if (parentDir != null) {
            // Search recursively in all folders to find the shared flow
            File result = searchRecursively(parentDir, folderNamePrefix);
            if (result != null) {
                currentFolder = result.getAbsolutePath();
                return result;
            }
        }

        // If still not found, throw an exception
        throw new IllegalArgumentException("No folder found starting with: " + folderNamePrefix);
    }

    private File searchRecursively(File directory, String folderNamePrefix) {
        File matchingFolder = findMatchingFolder(directory, folderNamePrefix);
        if (matchingFolder != null) {
            return matchingFolder;
        }

        return searchInSubFolders(directory, folderNamePrefix);
    }

    private File findMatchingFolder(File directory, String folderNamePrefix) {
        File[] matchingFolders = directory.listFiles((dir, name) -> name.startsWith(folderNamePrefix));
        if (matchingFolders != null && matchingFolders.length > 0) {
            return matchingFolders[0];
        }
        return null;
    }

    private File searchInSubFolders(File directory, String folderNamePrefix) {
        File[] subFolders = directory.listFiles(File::isDirectory);
        if (subFolders != null) {
            for (File subFolder : subFolders) {
                File result = searchRecursively(subFolder, folderNamePrefix);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public void setCurrentFolderToInitialState() {
        this.currentFolder = folderLocation.replaceAll("\\\\(apiproxy|sharedflowbundle)", "");
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

    @Override
    public Path findFirstChildFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] childFolders = folder.listFiles(File::isDirectory);
        if (childFolders != null && childFolders.length > 0) {
            return childFolders[0].toPath();
        } else {
            throw new IllegalArgumentException("No child folders found in the specified location.");
        }
    }


    @Override
    public void addValueToDictionaryMap(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            dictionaryMap.put(key, value);
        }
    }

    @Override
    public void dictionaryMapToCsv(String outputCsv) throws IOException {
        File file = new File(outputCsv);
        Map<String, String> existingEntries = new HashMap<>();

        // Read existing entries from the file
        if (file.exists()) {
            List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            for (String line : lines) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    existingEntries.put(parts[0], parts[1]);
                }
            }
        }

        // Write only new entries to the file
        try (FileWriter writer = new FileWriter(file, true)) { // 'true' enables appending to the file
            for (Map.Entry<String, String> entry : dictionaryMap.entrySet()) {
                if (!existingEntries.containsKey(entry.getKey())) {
                    writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                }
            }
            writer.flush(); // Ensure all data is written to the file
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
