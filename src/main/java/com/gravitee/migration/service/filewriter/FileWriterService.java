package com.gravitee.migration.service.filewriter;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface FileWriterService {

    /**
     * This method is used to save the given Gravitee JSON string to a file.
     *
     * @param graviteeJson The Gravitee JSON string to be saved.
     * @throws IOException If an I/O error occurs while writing to the file.
     */
    void saveJsonToFile(String graviteeJson) throws IOException;

    /**
     * This method is used to create a directory for exporting shared flows.
     *
     * @param jarDir The directory where the shared flow files will be exported.
     * @return The created directory.
     * @throws IOException If an I/O error occurs while creating the directory.
     */
    File createExportDirectory(String jarDir) throws IOException;

    /**
     * This method is used to generate a master script to execute all scripts of the shared flows at once
     *
     * @param sharedFlowDir The directory containing the shared flow files.
     * @throws IOException If an I/O error occurs while writing to the file.
     */
    void generateAndSaveMasterScript(File sharedFlowDir) throws IOException;

    /**
     * This method is used to generate a cURL script from the given Gravitee JSON and save it to a file.
     *
     * @param graviteeJson         The Gravitee JSON string.
     * @param sharedFlowName       The name of the shared flow.
     * @param sharedFlowExportFile The file where the cURL script will be saved.
     * @throws IOException If an I/O error occurs while writing to the file.
     */
    void generateAndSaveCurlScript(List<String> graviteeJson, String sharedFlowName, File sharedFlowExportFile) throws IOException;

    /**
     * This method is used to write the dictionary map to a file.
     *
     * @param key   The key to be added to the dictionary map.
     * @param value The value to be added to the dictionary map.
     */
    void addValueToDictionaryMap(String key, String value);

    /**
     * This method is used to write the dictionary map to a CSV file.
     *
     * @param outputCsv The path to the output CSV file.
     * @throws IOException If an I/O error occurs while writing to the file.
     */
    void dictionaryMapToCsv(String outputCsv) throws IOException;

}
