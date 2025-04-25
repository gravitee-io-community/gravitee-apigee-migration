package com.gravitee.migration.service.filereader;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service interface for reading and parsing files, specifically XML files.
 * This service provides methods to find specific directories and parse XML files within them.
 */
public interface FileReaderService {
    /**
     * Returns the absolute path of the directory if found.
     *
     * @param folderLocation The input folder location
     * @return The absolute path of the directory if found.
     * @throws IllegalArgumentException if the folder is not found
     */
    String findDirectory(String folderLocation, String folderName);

    /**
     * Parses XML files from the specified folder location and folder name.
     *
     * @param folderLocation The root folder location to search for XML files.
     * @param folderName     The name of the subfolder containing the XML files. If null or empty, uses the root folder.
     * @return A list of parsed XML documents.
     */
    List<Document> parseXmlFiles(String folderLocation, String folderName) throws ParserConfigurationException, IOException, SAXException;

    /**
     * Finds the folder starting with the specified prefix in the input folder location.
     *
     * @param folderNamePrefix The prefix to search for in folder names.
     * @return The first folder found that starts with the specified prefix.
     */
    File findFolderStartingWith(String folderNamePrefix);

    /**
     * Finds the first child folder of the specified folder.
     */
    void setCurrentFolderToInitialState();

    /**
     * Gets the current folder path.
     *
     * @return The current folder path.
     */
    String getCurrentFolder();

    /**
     * Finds a document by its name in the provided list of documents.
     *
     * @param documents The list of documents to search in.
     * @param name      The name of the document to find.
     * @return The found document, or null if not found.
     */
    Document findDocumentByName(List<Document> documents, String name);

    /**
     * Finds the first child folder of the specified folder.
     *
     * @param folderPath The path of the folder to search in.
     * @return The first child folder found.
     */
    Path findFirstChildFolder(String folderPath);

    /**
     * Parses JavaScript files from the specified folder location and folder name.
     *
     * @param folderLocation The root folder location to search for JavaScript files.
     * @param folderName     The name of the subfolder containing the JavaScript files. If null or empty, uses the root folder.
     * @return A map of JavaScript file names to their content.
     * @throws IOException if an error occurs while reading the files.
     */
    Map<String, String> parseJavaScriptFiles(String folderLocation, String folderName) throws IOException;

    /**
     * Adds a value to the dictionary map.
     *
     * @param key   The key to add to the dictionary map.
     * @param value The value to add to the dictionary map.
     */
    void addValueToDictionaryMap(String key, String value);

    /**
     * Converts the dictionary map to a CSV file, appending new entries if the file already exists.
     *
     * @param outputCsv The path to the output CSV file.
     * @throws IOException if an I/O error occurs while writing to the file.
     */
    void dictionaryMapToCsv(String outputCsv) throws IOException;

}
