package com.gravitee.migration.converter.policy.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.policy.PolicyConverter;
import com.gravitee.migration.service.filewriter.FileWriterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import java.util.Map;

import static com.gravitee.migration.util.constants.policy.PolicyConstants.ACCESS_ENTITY;

@Component
@RequiredArgsConstructor
public class AccessEntityConverter implements PolicyConverter {

    private final XPath xPath;
    private final FileWriterService fileWriterService;

    @Override
    public boolean supports(String policyType) {
        return ACCESS_ENTITY.equals(policyType);
    }

    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        var policyName = xPath.evaluate("/AccessEntity/@name", apiGeePolicy);

        fileWriterService.addValueToDictionaryMap(ACCESS_ENTITY.concat(".").concat(policyName), "changeme-application-information");
    }
}
