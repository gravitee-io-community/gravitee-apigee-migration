package com.gravitee.migration.converter.object;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * This class is responsible for mapping the documentation field in the Gravitee JSON.
 */
@Component
public class DocumentationObjectConverter {

    public void mapDocumentation(ObjectNode graviteeConfig) {
        var content = readDocumentationFileContent();

        if (!content.isEmpty()) {
            var pagesNode = graviteeConfig.putArray("pages");
            var pageNode = pagesNode.addObject();
            pageNode.put("id", UUID.randomUUID().toString());
            pageNode.put("name", "Homepage");
            pageNode.put("type", "SWAGGER");

            pageNode.put("content", content);

            pageNode.put("order", 0);
            pageNode.put("published", false);
            pageNode.put("visibility", "PUBLIC");
            pageNode.put("updatedAt", OffsetDateTime.now().toString());
            pageNode.put("contentType", "text/yaml");
            pageNode.put("homepage", true);
            pageNode.put("parentPath", "/");
            pageNode.put("excludedAccessControls", false);
            pageNode.putArray("accessControls");
        }
    }

    // mocked documentation
    private String readDocumentationFileContent() {
        try {
            var openApiYamlPath = new ClassPathResource("openapi.yaml").getFile().toPath();
            return Files.readString(openApiYamlPath).replace("\n", "\r\n").replace("\r", "");
        } catch (IOException e) {
            // File not found or other IO exception, do nothing and return an empty string
            return "";
        }
    }
}