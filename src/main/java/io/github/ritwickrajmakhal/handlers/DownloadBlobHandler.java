package io.github.ritwickrajmakhal.handlers;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

/**
 * Handler for downloading blobs from Azure Blob Storage.
 * <p>
 * This handler accepts blob name and destination path parameters and:
 * <ul>
 * <li>Downloads the specified blob from Azure Blob Storage</li>
 * <li>Saves the blob content to the specified local path</li>
 * <li>Returns a JSON response indicating success or failure</li>
 * </ul>
 * <p>
 * Expected JSON input:
 * 
 * <pre>
 * {
 *   "blobName": "name-of-blob.ext",
 *   "destinationPath": "local/path/to/save/file"
 * }
 * </pre>
 * <p>
 * Response JSON:
 * 
 * <pre>
 * {
 *   "success": true|false,
 *   "message": "Success message", (if successful)
 *   "error": "Error message" (if failed)
 * }
 * </pre>
 */
public class DownloadBlobHandler implements FunctionHandler {
    /** Client for Azure Blob Storage operations */
    private final BlobClient blobClient;

    /** JSON object mapper for serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new DownloadBlobHandler with the specified blob client.
     *
     * @param blobClient The client for Azure Blob Storage operations
     */
    public DownloadBlobHandler(final BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    /**
     * Executes the download blob operation with the provided arguments.
     * <p>
     * This method:
     * <ol>
     * <li>Validates that both blobName and destinationPath parameters are
     * provided</li>
     * <li>Attempts to download the specified blob to the destination path</li>
     * <li>Returns a JSON response with the results of the operation</li>
     * </ol>
     * <p>
     * If the destination path is a directory, the blob will be saved with its
     * original name
     * in that directory. If it's a file path, the blob content will be saved to
     * that file.
     *
     * @param args A JsonNode containing the "blobName" and "destinationPath"
     *             parameters
     * @return A JSON string containing the operation results
     * @throws Exception If there is an error during JSON serialization
     */
    @Override
    public String execute(final JsonNode args) throws Exception {
        final String blobName = args.has("blobName") ? args.get("blobName").asText() : "";
        final String destinationPath = args.has("destinationPath") ? args.get("destinationPath").asText() : "";

        if (blobName.isEmpty() || destinationPath.isEmpty()) {
            final Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Missing required parameters: blobName and/or destinationPath");
            return mapper.writeValueAsString(response);
        }

        try {
            blobClient.downloadBlob(blobName, destinationPath);

            final Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blob '" + blobName + "' downloaded successfully to: " + destinationPath);
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            final Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to download blob: " + e.getMessage());
            return mapper.writeValueAsString(response);
        }
    }
}