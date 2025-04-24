package io.github.ritwickrajmakhal.interfaces;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for handling function calls from OpenAI API
 */
public interface FunctionHandler {
    /**
     * Executes a function based on the provided arguments
     * @param args The arguments provided by the OpenAI model
     * @return A JSON string representing the function's result
     * @throws Exception If an error occurs during execution
     */
    String execute(JsonNode args) throws Exception;
}