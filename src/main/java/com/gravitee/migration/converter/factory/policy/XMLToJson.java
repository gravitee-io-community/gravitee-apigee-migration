package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import java.util.Map;

import static com.gravitee.migration.util.GraviteeCliUtils.createBasePhaseObject;
import static com.gravitee.migration.util.StringUtils.readGroovyPolicy;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.XML_TO_JSON;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.GROOVY;

/**
 * <p>Converts XMLToJSON policy from Apigee to Gravitee.</p>
 *
 * <p>This class implements the PolicyConverter interface and provides the logic
 * to convert the XMLToJSON policy. Uses groovy custom policy to have all features that the Apigee
 * XMLToJSON policy offers</p>
 */
@Component
@RequiredArgsConstructor
public class XMLToJson implements PolicyConverter {

    @Value("${groovy.xml-to-json}")
    private String xmlToJsonGroovyFileLocation;

    private final XPath xPath;

    private static final String WARNING = """
            ##############################################################
            #                      SECURITY WARNING                      #
            ##############################################################
            - You are using the XMLtoJSON policy, which is a Groovy script.
            - In order for the policy to work the following security configurations need to be added in the groovy sandbox
            - class groovy.xml.XmlSlurper
            - class groovy.xml.slurpersupport.GPathResult
            - class groovy.xml.slurpersupport.NodeChild
            """;

    @Override
    public boolean supports(String policyType) {
        return XML_TO_JSON.equals(policyType);
    }

    /**
     * Converts the XMLToJSON policy from Apigee to Gravitee.
     * Groovy policy to match the XMLToJSON policy in Apigee.
     *
     * @param condition     The condition to be applied to the policy.
     * @param apiGeePolicy The policy document.
     * @param phaseArray   The array node to which the converted policy will be added (e.g., request or response).
     * @param phase        The phase of the policy (e.g., request, response).
     * @throws Exception if an error occurs during conversion.
     */
    @Override
    public void convert(String condition, Document apiGeePolicy, ArrayNode phaseArray, String phase, Map<String, String> conditionMappings) throws Exception {
        System.out.println(WARNING);
        // Extract values
        var name = xPath.evaluate("/XMLToJSON/@name", apiGeePolicy);
        var recognizeNumber = Boolean.parseBoolean(xPath.evaluate("/XMLToJSON/Options/RecognizeNumber", apiGeePolicy));
        var recognizeBoolean = Boolean.parseBoolean(xPath.evaluate("/XMLToJSON/Options/RecognizeBoolean", apiGeePolicy));
        var recognizeNull = Boolean.parseBoolean(xPath.evaluate("/XMLToJSON/Options/RecognizeNull", apiGeePolicy));
        var nullValue = xPath.evaluate("/XMLToJSON/Options/NullValue", apiGeePolicy);
        var stripLevel = Integer.valueOf(xPath.evaluate("/XMLToJSON/Options/StripLevels", apiGeePolicy));
        var treatAsArray = (NodeList) xPath.evaluate("/XMLToJSON/Options/TreatAsArray/Path", apiGeePolicy, XPathConstants.NODESET);

        // Construct arrays string
        String treatAsArrayString = convertNodeListToCommaSeparatedString(treatAsArray);

        // Construct the script
        var script = createGroovyPolicy(phase, nullValue, recognizeNumber, recognizeBoolean, recognizeNull, stripLevel, treatAsArrayString);

        // Create a base scope node for the policy
        createBaseObjectObject(condition, name, phaseArray, phase, script, conditionMappings);
    }

    private void createBaseObjectObject(String condition, String name, ArrayNode scopeArray, String phase, String script, Map<String, String> conditionMappings) throws XPathExpressionException {
        // Create a base scope node for the policy
        var scopeNode = createBasePhaseObject(condition, name, GROOVY, scopeArray, conditionMappings);

        var configurationObject = scopeNode.putObject(CONFIGURATION);
        configurationObject.put(READ_CONTENT, true);
        configurationObject.put(OVERRIDE_CONTENT, true);
        configurationObject.put(SCOPE, phase.toUpperCase());
        configurationObject.put(SCRIPT, script);
    }

    private String createGroovyPolicy(String scope, String nullValueInput, boolean recognizeNumber, boolean recognizeBoolean, boolean recognizeNull,
                                      Integer stripLevels, String treatAsArray) throws Exception {

        // Read the Groovy script from the specified text file
        var policyString = readGroovyPolicy(xmlToJsonGroovyFileLocation);
        // Handle null value input, if it is not present the policy should act different(explained in XMLtoJSON Apigee documentation)
        var nullValue = handleNullValueInput(nullValueInput);

        return String.format(
                policyString,
                scope,
                nullValue,
                recognizeNumber,
                recognizeBoolean,
                recognizeNull,
                stripLevels,
                treatAsArray
        );
    }

    private Object handleNullValueInput(String nullValueInput) {
        return (nullValueInput == null || nullValueInput.isEmpty()) ? null : nullValueInput;
    }

    private String convertNodeListToCommaSeparatedString(NodeList nodeList) {
        // Convert NodeList to a comma-separated string that will be added to the groovy script (ex. output ["Array/test", "Array/result"])
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append("\"").append(nodeList.item(i).getTextContent().trim()).append("\"");
        }
        result.append("]");
        return result.toString();
    }
}
