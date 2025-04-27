package com.gravitee.migration.service.filereader;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service interface for reading and parsing files.
 */
public interface FileReaderService {

    /**
     * Reads the absolute path of a directory based on the provided folder location and folder name.
     *
     * @param folderLocation The root folder location.
     * @param folderName     The name of the subfolder.
     * @return The absolute path of the specified directory.
     */
    String readAbsolutePathOfDirectory(String folderLocation, String folderName);

    /**
     * Finds a folder that starts with the specified prefix within the given shared flows folder.
     *
     * @param sharedFlowsFolder The root folder location to search for subfolders.
     * @param folderNamePrefix  The prefix of the folder name to search for.
     * @return The found folder, or null if not found.
     */
    File findFolderStartingWith(String sharedFlowsFolder, String folderNamePrefix);

    /**
     * Parses XML files from the specified folder location and folder name.
     *
     * @param folderLocation The root folder location to search for XML files.
     * @param folderName     The name of the subfolder containing the XML files. If null or empty, uses the root folder.
     * @return A list of parsed XML documents.
     */
    List<Document> readFiles(String folderLocation, String folderName) throws ParserConfigurationException, IOException, SAXException;

    /**
     * Finds a document by its name in the provided list of documents.
     *
     * @param documents The list of documents to search in.
     * @param name      The name of the document to find.
     * @return The found document, or null if not found.
     */
    Document findDocumentByName(List<Document> documents, String name);

    /**
     * Parses JavaScript files from the specified folder location and folder name.
     *
     * @param folderLocation The root folder location to search for JavaScript files.
     * @param folderName     The name of the subfolder containing the JavaScript files. If null or empty, uses the root folder.
     * @return A map of JavaScript file names to their content.
     * @throws IOException if an error occurs while reading the files.
     */
    Map<String, String> readJavaScriptFiles(String folderLocation, String folderName) throws IOException;

}
