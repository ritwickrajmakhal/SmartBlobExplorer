package io.github.ritwickrajmakhal.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for listing files in the local file system.
 * <p>
 * This handler accepts directory path and pattern parameters and:
 * <ul>
 * <li>Lists all files and directories in the specified directory</li>
 * <li>Optionally filters files by a regex pattern</li>
 * <li>Returns a JSON response with all matching file paths</li>
 * </ul>
 * <p>
 * Expected JSON input:
 * 
 * <pre>
 * {
 *   "directoryPath": "Downloads", (or any path like "~/Documents", etc.)
 *   "pattern": "*.pdf" (optional regex pattern)
 *   "includeDirectories": true (optional, whether to include directories in results)
 * }
 * </pre>
 * <p>
 * Response JSON:
 * 
 * <pre>
 * {
 *   "success": true|false,
 *   "message": "Success message", (if successful)
 *   "error": "Error message", (if failed)
 *   "files": ["path/to/file1", "path/to/file2", ...] (if successful)
 *   "directories": ["path/to/dir1", "path/to/dir2", ...] (if includeDirectories=true)
 * }
 * </pre>
 */
public class ListLocalFilesHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(ListLocalFilesHandler.class);

    /** Client for blob storage operations */
    private final BlobClient blobClient;

    /** JSON object mapper for serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new ListLocalFilesHandler with the specified client.
     *
     * @param blobClient The client for Azure Blob Storage operations
     */
    public ListLocalFilesHandler(BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    /**
     * Executes the list local files operation with the provided arguments.
     * <p>
     * This method:
     * <ol>
     * <li>Extracts the directory path and optional pattern filter from the
     * arguments</li>
     * <li>Lists all files and optionally directories in the specified
     * directory</li>
     * <li>Returns a JSON response with the results</li>
     * </ol>
     *
     * @param args A JsonNode containing the "directoryPath" parameter and optional
     *             "pattern" parameter
     * @return A JSON string containing the operation results
     * @throws Exception If there is an error during JSON serialization
     */
    @Override
    public String execute(final JsonNode args) throws Exception {
        final String directoryPath = args.has("directoryPath") ? args.get("directoryPath").asText() : "";
        final String pattern = args.has("pattern") ? args.get("pattern").asText() : null;
        final boolean includeDirectories = !args.has("includeDirectories") || args.get("includeDirectories").asBoolean();

        if (directoryPath.isEmpty()) {
            final Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "No directory path provided");
            return mapper.writeValueAsString(response);
        }

        logger.info("Listing local files in directory: {} with pattern: {}, includeDirectories: {}",
                directoryPath, pattern != null ? pattern : "none", includeDirectories);

        try {
            // Use the BlobClient to list local files and directories
            final Map<String, List<String>> listing = blobClient.listLocalFilesAndDirs(
                    directoryPath, pattern, includeDirectories);

            final List<String> files = listing.get("files");
            final List<String> directories = listing.get("directories");

            final Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Found %d files and %d directories in %s",
                    files.size(), directories.size(), directoryPath));
            response.put("files", files);

            if (includeDirectories) {
                response.put("directories", directories);
            }

            // Add some additional info to make the response more useful
            if (!files.isEmpty() || !directories.isEmpty()) {
                response.put("path", directoryPath);
                response.put("totalItems", files.size() + directories.size());
            }

            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error listing files in directory: {}", directoryPath, e);

            final Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to list files: " + e.getMessage());

            return mapper.writeValueAsString(response);
        }
    }
}