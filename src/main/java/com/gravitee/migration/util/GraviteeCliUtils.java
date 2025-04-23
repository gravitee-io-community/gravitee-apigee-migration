package com.gravitee.migration.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.qos.logback.core.util.StringUtil.isNullOrEmpty;
import static com.gravitee.migration.util.StringUtils.isNotNullOrEmpty;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.Common.*;
import static com.gravitee.migration.util.constants.GraviteeCliConstants.PolicyType.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GraviteeCliUtils {

    private static final String ROOT_PATH = "/";

    /**
     * Creates a base phase object for the policy with the specified name, condition, and type. (object in the request or response phase)
     *
     * @param policyName The name of the policy.
     * @param condition  The condition to be applied to the policy.
     * @param policyType The type of the policy (e.g., JavaScript, Groovy).
     * @param phaseArray The array node to which the converted policy will be added (e.g., request, response).
     * @return An ObjectNode representing the base phase object.
     */
    public static ObjectNode createBasePhaseObject(String condition, String policyName, String policyType, ArrayNode phaseArray,
                                                   Map<String, String> conditionMappings) {
        ObjectNode phaseObject = phaseArray.addObject();
        phaseObject.put(NAME, policyName);

        if (isNotNullOrEmpty(condition)) {
            phaseObject.put(CONDITION, convertApigeeConditionToGravitee(condition, conditionMappings));
        }

        phaseObject.put(ENABLED, isPolicyEnabled(policyType));
        phaseObject.put(POLICY, policyType);

        return phaseObject;
    }

    /**
     * Creates a Groovy configuration object inside the phaseObject and populates it with the provided script and scope.
     *
     * @param script      The Groovy script to be executed.
     * @param phase       The scope of the configuration (e.g., request, response).
     * @param phaseObject The object node representing the scope.
     */
    public static void createGroovyConfiguration(String script, String phase, ObjectNode phaseObject) {
        // Crate configuration object and set properties
        var configurationObject = phaseObject.putObject(CONFIGURATION);
        configurationObject.put(SCOPE, phase.toUpperCase());
        configurationObject.put(READ_CONTENT, true);
        configurationObject.put(OVERRIDE_CONTENT, false);

        // Populate groovy script
        var generatedScript = String.format(script, phase);
        configurationObject.put(SCRIPT, generatedScript);
    }

    /**
     * Converts an Apigee policy condition to a Gravitee-compatible condition.
     * This method uses a mapping file to translate specific conditions. (Hardcoded)
     *
     * @param apigeeCondition     The Apigee condition to be converted.
     * @param translationMappings The map containing the translation mappings.
     * @return The converted Gravitee condition, or null if the input is null or empty.
     */
    public static String convertApigeeConditionToGravitee(String apigeeCondition, Map<String, String> translationMappings) {
        if (apigeeCondition == null || apigeeCondition.trim().isEmpty()) {
            return null;
        }

        // Replace Apigee-specific syntax with Gravitee syntax using the map
        String result = apigeeCondition.trim();
        for (Map.Entry<String, String> entry : translationMappings.entrySet()) {
            if (result.equals(entry.getKey())) { // Check if the result matches the entry key
                result = entry.getValue(); // Replace the result with the corresponding value
                break; // Exit the loop since a match is found
            }
        }

        return result;
    }

    /**
     * Constructs the condition for the selectors object based on the provided condition string. (used for constructing flow conditions)
     *
     * @param condition The condition string to be parsed and converted.
     * @param selectorsObject The selectors object to which the condition will be added.
     * @param conditionMappings The map containing the translation mappings for conditions.
     */
    public static void constructCondition(String condition, ObjectNode selectorsObject, Map<String, String> conditionMappings, ArrayNode selectorsArray) {
        if (isNullOrEmpty(condition)) {
            // Default condition when no specific condition is provided
            constructDefaultCondition(selectorsObject);
        } else {
            // Parse and construct specific condition
            constructSpecificCondition(condition, selectorsObject, conditionMappings, selectorsArray);
        }
    }

    private static void constructDefaultCondition(ObjectNode selectorsObject) {
        // If no condition is provided, set default values (everything will pass via this flow)
        selectorsObject.put(TYPE, HTTP);
        selectorsObject.put(PATH, ROOT_PATH);
        selectorsObject.put(PATH_OPERATOR, STARTS_WITH);
    }

    private static void constructSpecificCondition(String condition, ObjectNode selectorsObject, Map<String, String> conditionMappings, ArrayNode selectorsArray) {
        var convertedCondition = convertApigeeConditionToGravitee(condition, conditionMappings);

        if(isNullOrEmpty(convertedCondition)) {
            return;
        }

        List<String> paths = extractPathsFromCondition(convertedCondition);
        List<String> methods = extractMethodsFromCondition(convertedCondition);

        // Add methods to the selectors object
        addMethodsToSelectors(selectorsObject, methods);

        // If there is only one path in the condition, set it as the path in the flow selectors
        if (paths.size() == 1) {
            setSinglePathCondition(selectorsObject, paths.getFirst(), convertedCondition);
        } else {
            setMultiplePathsCondition(selectorsObject, convertedCondition, selectorsArray);
        }
    }

    private static void addMethodsToSelectors(ObjectNode selectorsObject, List<String> methods) {
        if(methods.isEmpty()) {
            return; // No methods to add
        }

        // Store the methods from the condition in the selectors
        ArrayNode methodsArray = selectorsObject.putArray(METHODS);
        for (String method : methods) {
            methodsArray.add(method.toUpperCase());
        }
    }

    private static void setSinglePathCondition(ObjectNode selectorsObject, String path, String condition) {
        // Set the path in the selectors
        selectorsObject.put(TYPE, HTTP);
        selectorsObject.put(PATH, path);
        selectorsObject.put(PATH_OPERATOR, EQUALS);

        // Remove paths and methods from the condition
        String remainingCondition = removePathsAndMethodsFromCondition(condition);

        // If there is any valid remaining condition, set it
        if (!remainingCondition.isEmpty()) {
            selectorsObject.put(TYPE, CONDITION.toUpperCase());
            selectorsObject.put(CONDITION, remainingCondition);
        }
    }


    private static void setMultiplePathsCondition(ObjectNode selectorsObject, String condition, ArrayNode selectorsArray) {
        // Remove methods from the condition, as they are already set in the selectors
        String result = removeMethodsFromCondition(condition);

        selectorsObject.put(TYPE, HTTP);
        selectorsObject.put(PATH, "/");
        selectorsObject.put(PATH_OPERATOR, "STARTS_WITH");

        // Set the remaining condition
        var conditionsObjectSelector = selectorsArray.addObject();
        conditionsObjectSelector.put(TYPE, CONDITION.toUpperCase());
        conditionsObjectSelector.put(CONDITION, result.trim());
    }

    private static List<String> extractPathsFromCondition(String condition) {
        // Extract the paths from the condition using regex
        List<String> paths = new ArrayList<>();
        Pattern pathPattern = Pattern.compile("#request\\.pathInfo\\s+matches\\s+'([^']+)'");
        Matcher matcher = pathPattern.matcher(condition);

        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        return paths;
    }

    private static List<String> extractMethodsFromCondition(String condition) {
        // Extract the methods from the condition using regex
        List<String> methods = new ArrayList<>();
        Pattern methodPattern = Pattern.compile("#request\\.method\\s*==\\s*\"([^\"]+)\"");
        Matcher matcher = methodPattern.matcher(condition);

        while (matcher.find()) {
            methods.add(matcher.group(1));
        }
        return methods;
    }

    private static String removeMethodsFromCondition(String condition) {
        // Remove all method conditions from the condition string using regex
        String result = condition.replaceAll("#request\\.method\\s*==\\s*\"[^\"]+\"", "");

        // Remove any dangling logical operators (&& or ||) that are not followed by valid conditions
        result = result.replaceAll("(\\s*(&&|\\|\\|)\\s*)+(?=\\s*\\})", "").trim();

        // Remove empty braces if present
        result = result.replaceAll("^\\{\\s*\\}$", "").trim();

        return result;
    }

    private static String removePathsAndMethodsFromCondition(String condition) {
        // Remove all path conditions
        String result = condition.replaceAll("#request\\.pathInfo\\s+matches\\s+'[^']+'", "");

        // Remove all method conditions
        result = result.replaceAll("#request\\.method\\s*==\\s*\"[^\"]+\"", "");

        // Remove leftover logical operators (e.g., &&, ||) and trim
        result = result.replaceAll("(\\s*(&&|\\|\\|)\\s*)+", "").trim();

        // Remove empty braces if present
        result = result.replaceAll("^\\{\\s*\\}$", "").trim();

        // Return the cleaned condition or an empty string if nothing is left
        return result.isEmpty() ? "" : result;
    }

    private static boolean isPolicyEnabled(String policyType) {
        // Disable the policy if it is one of the following types
        return !(policyType.equals(JAVASCRIPT) || policyType.equals(API_KEY) || policyType.equals(JWT));
    }
}
