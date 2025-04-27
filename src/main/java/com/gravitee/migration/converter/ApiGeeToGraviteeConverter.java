package com.gravitee.migration.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gravitee.migration.converter.object.ApiObjectConverter;
import com.gravitee.migration.converter.object.DocumentationObjectConverter;
import com.gravitee.migration.converter.object.PlanObjectConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;

/**
 * Converts Apigee API to Gravitee API.
 * This class is responsible for converting the API configuration from Apigee format to Gravitee format.
 */
@Component
@RequiredArgsConstructor
public class ApiGeeToGraviteeConverter {

    private final ObjectMapper objectMapper;
    private final XPath xPath;
    private final ApiObjectConverter apiObjectConverter;
    private final DocumentationObjectConverter documentationObjectConverter;
    private final PlanObjectConverter planObjectConverter;


    /**
     * Converts the Apigee API configuration to Gravitee API configuration.
     *
     * @param rootXml         The root XML document of the Apigee API.
     * @param proxyXml        The proxy XML document of the Apigee API.
     * @param apiGeePolicies  The list of policies associated with the Apigee API.
     * @param targetEndpoints The list of target endpoints associated with the Apigee API.
     * @return The Gravitee API configuration as a JSON string.
     * @throws XPathExpressionException If there is an error evaluating the XPath expression.
     * @throws IOException              If there is an error writing the JSON output.
     */
    public String apiGeeToGraviteeConverter(Document rootXml, Document proxyXml, List<Document> apiGeePolicies, List<Document> targetEndpoints, String apiProxyFolderLocation) throws XPathExpressionException, IOException {
        var graviteeConfig = objectMapper.createObjectNode();
        var planName = xPath.evaluate("/APIProxy/@name", rootXml);

        apiObjectConverter.mapApiObject(rootXml, proxyXml, graviteeConfig, targetEndpoints);
        planObjectConverter.createPlan(graviteeConfig, planName, apiGeePolicies, targetEndpoints, proxyXml, apiProxyFolderLocation);
        documentationObjectConverter.mapDocumentation(graviteeConfig);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graviteeConfig);
    }
}