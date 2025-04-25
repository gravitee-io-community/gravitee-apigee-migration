package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import com.gravitee.migration.converter.factory.dto.JsonToXmlDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.readGroovyPolicy;
import static com.gravitee.migration.util.constants.CommonConstants.*;
import static com.gravitee.migration.util.constants.policy.PolicyConstants.JSON_TO_XML;
import static com.gravitee.migration.util.constants.policy.PolicyTypeConstants.GROOVY;

/**
 * <p>Converts JSONToXML policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the JSONToXML policy. Uses groovy custom policy to have all features that the Apigee
 * JSONToXML policy offers</p>
 */
@Component
@RequiredArgsConstructor
public class JsonToXmlConverter implements PolicyConverter {

    @Value("${groovy.json-to-xml}")
    private String jsonToXmlGroovyFileLocation;

    private final XPath xPath;

    private static final String WARNING = """
            ##############################################################
            #                      SECURITY WARNING                      #
            ##############################################################
            - You are using the JSONToXML policy, which is a Groovy script.
            - In order for the policy to work the following security configurations need to be added in the groovy sandbox
            - class groovy.xml.XmlSlurper
            - class groovy.xml.slurpersupport.GPathResult
            - class groovy.xml.slurpersupport.NodeChild
            """;

    @Override
    public boolean supports(String policyType) {
        return JSON_TO_XML.equals(policyType);
    }

    /**
     * Converts the JSONToXML policy from Apigee to Gravitee.
     * Groovy policy to match the JSONToXML policy in Apigee.
     *
     * @param condition      The condition to be applied to the policy.
     * @param apiGeePolicy The policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request, response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        System.out.println(WARNING);
        // Extract values
        var properties = new JsonToXmlDto(
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

        // Create a base scope node for the policy
        createScopeObject(condition, phaseArray, phase, properties, conditionMappings);
    }

    private void createScopeObject(String condition, ArrayNode phaseArray, String phase, JsonToXmlDto properties, Map<String, String> conditionMappings) throws Exception {
        // Create a base scope node for the policy
        var phaseObjectNode = createBasePhaseObject(condition, properties.name(), GROOVY, phaseArray, conditionMappings);

        // Create the configuration object
        var configurationObject = phaseObjectNode.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());
        configurationObject.put(OVERRIDE_CONTENT, true);
        configurationObject.put(READ_CONTENT, true);

        // Construct and insert script
        var script = generateGroovyScript(properties, phase);
        configurationObject.put(SCRIPT, script);
    }

    private String generateGroovyScript(JsonToXmlDto config, String phase) throws Exception {
        // Read the Groovy script from the specified text file
        var policyString = readGroovyPolicy(jsonToXmlGroovyFileLocation);

        // If textNameNode is not specified it should be empty, based on this input the policy will act different(specified in the documentation in JSONToXML in Apigee)
        String textNodeInput = getTextNodeInput(config);

        // Replace placeholders in the Groovy script with actual values
        return String.format(
                policyString,
                textNodeInput,
                config.nullValue(),
                config.objectRootElementName(),
                config.arrayRootElementName(),
                config.arrayItemElementName(),
                config.attributePrefix(),
                config.attributeBlockName(),
                config.namespaceBlockName(),
                config.defaultNamespaceNodeName(),
                config.namespaceSeparator(),
                config.invalidCharsReplacement(),
                phase
        );
    }

    private String getTextNodeInput(JsonToXmlDto config) {
        if (config.textNodeName() == null || config.textNodeName().isEmpty()) {
            return "";
        }
        return config.textNodeName();
    }
}
