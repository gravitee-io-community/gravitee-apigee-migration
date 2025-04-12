package com.gravitee.migration.converter.factory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Interface for converting policies from one format to another.
 * Implementations of this interface should provide the logic to convert specific types of policies.
 */
public interface PolicyConverter {

    boolean supports(String policyType);

    void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception;
}
