package org.codephy.mappers.beast2;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility methods for extracting values from Codephy JSON.
 */
public class Utils {

    /**
     * Extract a numeric value from a parameter node.
     *
     * @param paramsNode JSON node containing parameters
     * @param paramName Name of the parameter to extract
     * @return The numeric value of the parameter
     */
    public static double extractNumericValue(JsonNode paramsNode, String paramName) {
        JsonNode valueNode = paramsNode.path(paramName);
        if (valueNode.isNumber()) {
            return valueNode.asDouble();
        } else if (valueNode.isObject() && valueNode.has("expression")) {
            // For expressions, we would need to evaluate them...
            // For simplicity, returning a default
            return 1.0;
        } else {
            return 1.0; // Default value
        }
    }
    
    /**
     * Extract a variable reference from a parameter node.
     *
     * @param paramsNode JSON node containing parameters
     * @param paramName Name of the parameter to extract reference from
     * @return The name of the referenced variable
     */
    public static String extractVariableReference(JsonNode paramsNode, String paramName) {
        JsonNode valueNode = paramsNode.path(paramName);
        if (valueNode.isObject() && valueNode.has("variable")) {
            return valueNode.path("variable").asText();
        } else if (valueNode.isTextual()) {
            return valueNode.asText();
        } else {
            throw new IllegalArgumentException("Cannot extract variable reference from parameter: " + paramName);
        }
    }
    
    /**
     * Extract dimension from a parameter node.
     *
     * @param paramsNode JSON node containing parameters
     * @return The dimension value or a default
     */
    public static int extractDimension(JsonNode paramsNode) {
        // Get dimension if provided
        int dimension = 1; // Default dimension
        if (paramsNode.has("dimension")) {
            if (paramsNode.path("dimension").isInt()) {
                dimension = paramsNode.path("dimension").asInt();
            } else if (paramsNode.path("dimension").isObject() && 
                       paramsNode.path("dimension").has("expression")) {
                // For expressions like getBranchCount(tree), we would need to evaluate them
                // For simplicity, using a default value
                dimension = 10; // Placeholder
            } else if (paramsNode.path("dimension").isObject() && 
                       paramsNode.path("dimension").has("variable")) {
                // For variable references, we would need to look up the value
                // For simplicity, using a default value
                dimension = 10; // Placeholder
            }
        }
        return dimension;
    }
    
    /**
     * Check if a variable name exists in a JSON node's fields.
     *
     * @param node JSON node to check
     * @param varName Variable name to look for
     * @return true if the variable exists
     */
    public static boolean variableExists(JsonNode node, String varName) {
        return node.has(varName);
    }
}