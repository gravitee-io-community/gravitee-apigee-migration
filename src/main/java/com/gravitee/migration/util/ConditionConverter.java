package com.gravitee.migration.util;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConditionConverter {

    // TODO: CHANGE LOGIC DEPENDING ON NEW EXAMPLES
    public static String convertApigeeConditionToGravitee(String apigeeCondition) {
        if (apigeeCondition == null || apigeeCondition.isEmpty()) {
            return "";
        }

        // Remove parentheses if any
        apigeeCondition = apigeeCondition.replace("(", "").replace(")", "");

        // Replace Apigee condition syntax with Gravitee condition syntax
        String graviteeCondition = apigeeCondition
                .replaceAll("\\s*request\\.verb\\s*", "#request.method")
                .replaceAll("\\s*request\\.header\\.\\s*", "#request.headers['")
                .replaceAll("\\s*!=\\s*null\\s*", "'] != null")
                .replaceAll("\\s*==\\s*", " == '")
                .replaceAll("\\s*AND\\s*", "' && ");

        return "{" + graviteeCondition + "}";
    }

    public static void constructCondition(String condition, ObjectNode selectorsObject) {
        if (condition == null || condition.trim().isEmpty()) {
            // Default condition when no specific condition is provided
            constructDefaultCondition(selectorsObject);
        } else {
            // Parse and construct specific condition
            constructSpecificCondition(condition, selectorsObject);
        }
    }

    private static void constructDefaultCondition(ObjectNode selectorsObject) {
        selectorsObject.put("type", "CONDITION");
        selectorsObject.put("condition", "true");
    }

    private static void constructSpecificCondition(String condition, ObjectNode selectorsObject) {
        List<String> paths = extractPathsFromCondition(condition);
        List<String> methods = extractMethodsFromCondition(condition);

        if (paths.size() == 1) {
            setSinglePathCondition(selectorsObject, paths.get(0), condition);
        } else {
            setMultiplePathsCondition(selectorsObject, paths);
        }

        addMethodsToSelectors(selectorsObject, methods);
    }

    private static void setSinglePathCondition(ObjectNode selectorsObject, String path, String condition) {
        selectorsObject.put("type", "HTTP");
        selectorsObject.put("path", path);
        selectorsObject.put("pathOperator", condition.contains("MatchesPath") ? "EQUALS" : "STARTS_WITH");
    }

    private static void setMultiplePathsCondition(ObjectNode selectorsObject, List<String> paths) {
        StringBuilder conditionBuilder = new StringBuilder("{");
        if (!paths.isEmpty()) {
            conditionBuilder.append("(");
            for (int i = 0; i < paths.size(); i++) {
                if (i > 0) {
                    conditionBuilder.append(" || ");
                }
                conditionBuilder.append("#request.path.matches('").append(paths.get(i)).append("')");
            }
            conditionBuilder.append(")");
        }
        conditionBuilder.append("}");
        selectorsObject.put("type", "CONDITION");
        selectorsObject.put("condition", conditionBuilder.toString());
    }

    private static void addMethodsToSelectors(ObjectNode selectorsObject, List<String> methods) {
        ArrayNode methodsArray = selectorsObject.putArray("methods");
        for (String method : methods) {
            methodsArray.add(method.toUpperCase());
        }
    }

    private static List<String> extractPathsFromCondition(String condition) {
        List<String> paths = new ArrayList<>();
        Pattern pathPattern = Pattern.compile("proxy\\.pathsuffix\\s+MatchesPath\\s+\"([^\"]+)\"");
        Matcher matcher = pathPattern.matcher(condition);

        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        return paths;
    }

    private static List<String> extractMethodsFromCondition(String condition) {
        List<String> methods = new ArrayList<>();
        Pattern methodPattern = Pattern.compile("request\\.verb\\s+=\\s+\"([^\"]+)\"");
        Matcher matcher = methodPattern.matcher(condition);

        while (matcher.find()) {
            methods.add(matcher.group(1));
        }
        return methods;
    }
}
