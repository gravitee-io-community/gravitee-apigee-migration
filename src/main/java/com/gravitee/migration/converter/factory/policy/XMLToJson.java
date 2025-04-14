package com.gravitee.migration.converter.factory.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import static com.gravitee.migration.util.GraviteeCliUtils.createBaseScopeNode;
import static com.gravitee.migration.util.StringUtils.readFileFromClasspath;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Policy.XML_TO_JSON;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.GROOVY;

/**
 * Converts XMLToJSON policy from APIgee to Gravitee.
 * This class implements the PolicyConverter interface and provides the logic to convert the XMLToJSON policy.
 */
@Component
@RequiredArgsConstructor
public class XMLToJson implements PolicyConverter {

    @Value("${groovy.xml-to-json}")
    private String xmlToJsonGroovyFileLocation;

    private final XPath xPath;

    @Override
    public boolean supports(String policyType) {
        return XML_TO_JSON.equals(policyType);
    }

    @Override
    public void convert(Node stepNode, Document apiGeePolicy, ArrayNode scopeArray, String scope) throws Exception {
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

        var scopeNode = createBaseScopeNode(stepNode, name, GROOVY, scopeArray);

        var configurationObject = scopeNode.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, scope.toUpperCase());
        var script = createGroovyPolicy(scope, nullValue, recognizeNumber, recognizeBoolean, recognizeNull, stripLevel, treatAsArrayString);
        configurationObject.put(SCRIPT, script);
    }

    private String createGroovyPolicy(String scope, String nullValueInput, boolean recognizeNumber, boolean recognizeBoolean, boolean recognizeNull,
                                      Integer stripLevels, String treatAsArray) throws Exception {

        var policyString = readFileFromClasspath(xmlToJsonGroovyFileLocation);
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
