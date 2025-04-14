package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.converter.factory.dto.JsonToXmlDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.StringUtils.readFileFromClasspath;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.JSON_TO_XML;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.GROOVY;

/**
 * Converts JSONToXML policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the JSONToXML policy.
 */
@Component
@RequiredArgsConstructor
public class JsonToXmlConverter implements PolicyConverter {

    @Value("${groovy.json-to-xml}")
    private String jsonToXmlGroovyFileLocation;

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return JSON_TO_XML.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
        var config = new JsonToXmlDto(
                xPath.evaluate("/JSONToXML/@name", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/NullValue", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/NamespaceBlockName", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/DefaultNamespaceNodeName", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/NamespaceSeparator", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/TextNodeName", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/AttributeBlockName", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/AttributePrefix", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/InvalidCharsReplacement", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/ObjectRootElementName", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/ArrayRootElementName", apiGeePolicy),
                xPath.evaluate("/JSONToXML/Options/ArrayItemElementName", apiGeePolicy)
        );
        var phaseObjectNode = createBaseScopeNode(stepNode, config.name(), GROOVY, scopeArray);

        var configurationObject = phaseObjectNode.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, scope.toUpperCase());
        configurationObject.put(OVERRIDE_CONTENT, true);
        configurationObject.put(READ_CONTENT, true);

        var script = generateGroovyScript(config, scope);
        configurationObject.put(SCRIPT, script);
    }

    public String generateGroovyScript(JsonToXmlDto config, String scope) throws Exception {

        var policyString = readFileFromClasspath(jsonToXmlGroovyFileLocation);

        return String.format(
                policyString,
                config.nullValue(),
                config.objectRootElementName(),
                config.arrayRootElementName(),
                config.arrayItemElementName(),
                config.attributePrefix(),
                config.attributeBlockName(),
                config.textNodeName(),
                config.namespaceBlockName(),
                config.defaultNamespaceNodeName(),
                config.namespaceSeparator(),
                config.invalidCharsReplacement(),
                scope
        );
    }
}
