package com.gravitee.migration.converter.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.w3c.dom.Document;

import java.util.Map;

/**
 * Interface for converting policies from one format to another.
 * Implementations of this interface should provide the logic to convert specific types of policies.
 */
public interface PolicyConverter {

    boolean supports(String policyType);

    void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception;
}
