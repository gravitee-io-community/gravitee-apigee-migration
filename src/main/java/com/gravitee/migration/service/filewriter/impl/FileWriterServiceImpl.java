package com.gravitee.migration.service.filewriter.impl;

import com.gravitee.migration.service.filewriter.FileWriterService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gravitee.migration.util.constants.groovy.GroovyConstants.CURL_COMMAND_TEMPLATE;
import static com.gravitee.migration.util.constants.groovy.GroovyConstants.MASTER_SCRIPT;

@Service
public class FileWriterServiceImpl implements FileWriterService {

    private static final String API_PROXY_OUTPUT = "gravitee_api_output.json";
    private static final String SHARED_FLOW_OUTPUT = "SharedFlowsExport";
    private static final String MASTER_SCRIPT_OUTPUT = "execute_all_scripts.bat";

    private final Map<String, String> dictionaryMap = new HashMap<>();

    @Override
    public void saveJsonToFile(String graviteeJson) throws IOException {
        // Write the JSON string to the specified output file.
        String jarDir = Paths.get("").toAbsolutePath().toString();
        File outputFile = new File(jarDir, API_PROXY_OUTPUT);
        try (OutputStream os = new FileOutputStream(outputFile)) {
            os.write(graviteeJson.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("Gravitee JSON configuration saved to: " + outputFile.getAbsolutePath());
    }

    @Override
    public File createExportDirectory(String jarDir) throws IOException {
        File exportDir = new File(jarDir, SHARED_FLOW_OUTPUT);
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + exportDir.getAbsolutePath());
        }
        return exportDir;
    }

    @Override
    public void generateAndSaveMasterScript(File sharedFlowDir) throws IOException {
        if (!sharedFlowDir.exists() || !sharedFlowDir.isDirectory()) {
            throw new IllegalArgumentException("The specified directory does not exist or is not a directory: " + sharedFlowDir.getAbsolutePath());
        }

        File masterScript = new File(sharedFlowDir, MASTER_SCRIPT_OUTPUT);
        try (FileWriter writer = new FileWriter(masterScript)) {
            // Replace placeholders in the MASTER_SCRIPT constant

            // Write the script content to the file
            writer.write(MASTER_SCRIPT);
        }
        System.out.println("Master script created at: " + masterScript.getAbsolutePath());
    }

    @Override
    public void generateAndSaveCurlScript(List<String> graviteeJson, String sharedFlowName, File sharedFlowExportFile) throws IOException {
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

    @Override
    public void addValueToDictionaryMap(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            dictionaryMap.put(key, value);
        }
    }

    @Override
    public void dictionaryMapToCsv(String outputCsv) throws IOException {
        File file = new File(outputCsv);
        Map<String, String> existingEntries = readExistingEntries(file);
        writeNewEntries(file, existingEntries);
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


    private Map<String, String> readExistingEntries(File file) throws IOException {
        Map<String, String> existingEntries = new HashMap<>();
        if (file.exists()) {
            List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            for (String line : lines) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    existingEntries.put(parts[0], parts[1]);
                }
            }
        }
        return existingEntries;
    }

    private void writeNewEntries(File file, Map<String, String> existingEntries) throws IOException {
        try (FileWriter writer = new FileWriter(file, true)) { // 'true' enables appending to the file
            for (Map.Entry<String, String> entry : dictionaryMap.entrySet()) {
                if (!existingEntries.containsKey(entry.getKey())) {
                    writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                }
            }
            writer.flush(); // Ensure all data is written to the file
        }
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


}
