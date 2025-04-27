package com.gravitee.migration.converter.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.w3c.dom.Document;

import java.util.Map;

public interface AdvancedPolicyConverter {

    boolean supports(String policyType);

    void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings, String currentFolderLocation) throws Exception;
}
