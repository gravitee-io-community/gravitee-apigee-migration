package com.gravitee.migration.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class GraviteeELTranslator {

    @Value("${gravitee.apigee-to-gravitee-conditions}")
    private String conditionMappingsFilePath;

    @Value("${gravitee.apigee-to-gravitee-assign-message-templates}")
    private String assignMessageTemplateMappingsFilePath;

    private static final Map<String, String> conditionMappings = new HashMap<>();
    private static final Map<String, String> assignMessageTemplateMappings = new HashMap<>();

    public Map<String, String> getConditionMappings() {
        return conditionMappings;
    }

    public Map<String, String> getAssignMessageTemplateMappings() {
        return assignMessageTemplateMappings;
    }

    public void loadConditionMappings() {
        loadMappings(conditionMappingsFilePath, conditionMappings);
    }

    public void loadAssignMessageTemplateMappings() {
        loadMappings(assignMessageTemplateMappingsFilePath, assignMessageTemplateMappings);
    }

    private void loadMappings(String filePath, Map<String, String> mappings) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + filePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                reader.lines()
                        .filter(line -> !line.trim().isEmpty() && line.contains("->"))
                        .forEach(line -> {
                            String[] parts = line.split("->", 2);
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            mappings.putIfAbsent(key, value);
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading mappings from resource: " + filePath, e);
        }
    }

}
