package io.github.ritwickrajmakhal;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

/**
 * Registry for managing function handlers used to process API requests.
 * <p>
 * This class provides a central registry for function handlers that are used to
 * process
 * function calls from clients. It maintains a mapping of function names to
 * their
 * corresponding handler implementations, allowing dynamic execution of
 * functions
 * based on the function name.
 * <p>
 * The registry supports:
 * <ul>
 * <li>Registering new function handlers</li>
 * <li>Executing functions by name with JSON arguments</li>
 * <li>Checking if a handler exists for a given function name</li>
 * </ul>
 */
public class FunctionCallRegistry {
    /** Map of function names to their handler implementations */
    private final Map<String, FunctionHandler> handlers = new HashMap<>();

    /** JSON object mapper for parsing arguments and serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Registers a handler for a specified function name.
     * <p>
     * This associates a function name with the handler that will process calls to
     * that function.
     * If a handler is already registered for the function name, it will be
     * replaced.
     *
     * @param functionName The name of the function to register
     * @param handler      The handler implementation for the function
     */
    public void registerHandler(final String functionName, final FunctionHandler handler) {
        handlers.put(functionName, handler);
    }

    /**
     * Executes a function with the specified name and arguments.
     * <p>
     * This method:
     * <ol>
     * <li>Retrieves the handler for the function name</li>
     * <li>Parses the JSON arguments</li>
     * <li>Invokes the handler's execute method with the parsed arguments</li>
     * <li>Returns the result as a JSON string</li>
     * </ol>
     * <p>
     * If no handler is registered for the function name, an error response is
     * returned.
     *
     * @param functionName The name of the function to execute
     * @param arguments    A JSON string containing the function arguments
     * @return A JSON string containing the function's result or an error message
     * @throws Exception If there is an error parsing the arguments or executing the
     *                   function
     */
    public String executeFunction(final String functionName, final String arguments) throws Exception {
        FunctionHandler handler = handlers.get(functionName);
        if (handler == null) {
            return mapper.writeValueAsString(Map.of(
                    "error", "No handler registered for function: " + functionName));
        }

        JsonNode args = mapper.readTree(arguments);
        return handler.execute(args);
    }

    /**
     * Checks if a handler is registered for the specified function name.
     *
     * @param functionName The name of the function to check
     * @return true if a handler is registered for the function name, false
     *         otherwise
     */
    public boolean hasHandler(final String functionName) {
        return handlers.containsKey(functionName);
    }
}