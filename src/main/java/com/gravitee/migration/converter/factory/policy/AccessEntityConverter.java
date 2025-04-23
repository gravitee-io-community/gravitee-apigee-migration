package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.service.filereader.FileReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;

import java.util.Map;

import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.ACCESS_ENTITY;

@Component
@RequiredArgsConstructor
public class AccessEntityConverter implements PolicyConverter {

    private final XPath xPath;
    private final FileReaderService fileReaderService;

    @Override
    public boolean supports(String policyType) {
        return ACCESS_ENTITY.equals(policyType);
    }

    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        var policyName = xPath.evaluate("/AccessEntity/@name", apiGeePolicy);

        fileReaderService.addValueToDictionaryMap(ACCESS_ENTITY.concat(".").concat(policyName), "changeme-application-information");
    }
}
