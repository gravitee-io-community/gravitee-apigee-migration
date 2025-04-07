package com.gravitee.migration.converter.factory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public interface PolicyConverter {

    boolean supports(String policyType);

    void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray) throws Exception;
}
