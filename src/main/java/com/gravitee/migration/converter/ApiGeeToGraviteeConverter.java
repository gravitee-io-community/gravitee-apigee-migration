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

@Component
@RequiredArgsConstructor
public class ApiGeeToGraviteeConverter {

    private final ObjectMapper objectMapper;
    private final XPath xPath;
    private final ApiObjectConverter apiObjectConverter;
    private final DocumentationObjectConverter documentationObjectConverter;
    private final PlanObjectConverter planObjectConverter;


    public String apiGeeToGraviteeConverter(Document rootXml, Document proxyXml, List<Document> apiGeePolicies, List<Document> targetEndpoints) throws XPathExpressionException, IOException {
        var graviteeConfig = objectMapper.createObjectNode();
        var planName = xPath.evaluate("/APIProxy/@name", rootXml);

        apiObjectConverter.mapApiObject(rootXml, proxyXml, graviteeConfig, targetEndpoints);
        planObjectConverter.createPlan(graviteeConfig, planName, apiGeePolicies, targetEndpoints, proxyXml);
        documentationObjectConverter.mapDocumentation(graviteeConfig);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graviteeConfig);
    }
}