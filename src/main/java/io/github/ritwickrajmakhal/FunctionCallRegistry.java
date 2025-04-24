package io.github.ritwickrajmakhal;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

/**
 * Registry for function handlers
 */
public class FunctionCallRegistry {
    private final Map<String, FunctionHandler> handlers = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Register a handler for a function
     */
    public void registerHandler(String functionName, FunctionHandler handler) {
        handlers.put(functionName, handler);
    }
    
    /**
     * Execute a function call
     */
    public String executeFunction(String functionName, String arguments) throws Exception {
        FunctionHandler handler = handlers.get(functionName);
        if (handler == null) {
            return mapper.writeValueAsString(Map.of(
                "error", "No handler registered for function: " + functionName
            ));
        }
        
        JsonNode args = mapper.readTree(arguments);
        return handler.execute(args);
    }
    
    /**
     * Check if a handler exists for a function
     */
    public boolean hasHandler(String functionName) {
        return handlers.containsKey(functionName);
    }
}