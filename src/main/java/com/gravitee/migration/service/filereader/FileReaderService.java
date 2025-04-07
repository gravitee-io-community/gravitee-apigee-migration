package com.gravitee.migration.service.filereader;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for reading and parsing files, specifically XML files.
 * This service provides methods to find specific directories and parse XML files within them.
 */
public interface FileReaderService {

    /**
     * Finds the "apiproxy" directory within the specified folder location.
     *
     * @param folderLocation The root folder location to search for the "apiproxy" directory.
     * @return The absolute path of the found "apiproxy" directory.
     * @throws IllegalArgumentException if no "apiproxy" directory is found.
     */
    String findApiProxyDirectory(String folderLocation);

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

    void setCurrentFolderToInitialState();

    String getCurrentFolder();

    Document findDocumentByName(List<Document> documents, String name);

    Path findFirstChildFolder(String folderPath);

}
