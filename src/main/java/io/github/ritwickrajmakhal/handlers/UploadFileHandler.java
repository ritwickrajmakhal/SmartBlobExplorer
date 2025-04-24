package io.github.ritwickrajmakhal.handlers;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

/**
 * Handler for uploading files to Azure Blob Storage and updating the search
 * index.
 * <p>
 * This handler accepts a file path parameter (which can be a local path or URL)
 * and:
 * <ul>
 * <li>Uploads the file to Azure Blob Storage via the BlobClient</li>
 * <li>Triggers a run of the Azure Search indexer to process the new blob</li>
 * <li>Returns a JSON response with the results of both operations</li>
 * </ul>
 * <p>
 * Expected JSON input:
 * 
 * <pre>
 * {
 *   "filePath": "path/to/file.ext" or "https://example.com/file.ext"
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
 *   "indexerRun": true|false, (if upload successful)
 *   "indexerMessage": "Indexer status message" (if upload successful)
 * }
 * </pre>
 */
public class UploadFileHandler implements FunctionHandler {
    /** Client for Azure Blob Storage operations */
    private final BlobClient blobClient;

    /** Client for Azure Search operations */
    private final AzureSearchClient searchClient;

    /** JSON object mapper for serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new UploadFileHandler with the specified clients.
     *
     * @param blobClient   The client for Azure Blob Storage operations
     * @param searchClient The client for Azure Search operations
     */
    public UploadFileHandler(BlobClient blobClient, AzureSearchClient searchClient) {
        this.blobClient = blobClient;
        this.searchClient = searchClient;
    }

    /**
     * Executes the upload file operation with the provided arguments.
     * <p>
     * This method:
     * <ol>
     * <li>Validates that a file path is provided</li>
     * <li>Uploads the file to Azure Blob Storage</li>
     * <li>If the upload is successful, triggers the Azure Search indexer</li>
     * <li>Returns a JSON response with the results</li>
     * </ol>
     *
     * @param args A JsonNode containing the "filePath" parameter
     * @return A JSON string containing the operation results
     * @throws Exception If there is an error during JSON serialization
     */
    @Override
    public String execute(final JsonNode args) throws Exception {
        final String filePath = args.has("filePath") ? args.get("filePath").asText() : "";

        if (filePath.isEmpty()) {
            final Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "No file path provided");
            return mapper.writeValueAsString(response);
        }

        final boolean success = blobClient.uploadFile(filePath);

        final Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            response.put("message", "File uploaded successfully: " + filePath);

            // Run the indexer after successful upload
            final boolean indexerSuccess = searchClient.runIndexer();
            if (indexerSuccess) {
                response.put("indexerRun", true);
                response.put("indexerMessage", "Search index updated successfully");
            } else {
                response.put("indexerRun", false);
                response.put("indexerMessage", "Failed to update search index");
            }
        } else {
            response.put("error", "Failed to upload file: " + filePath);
        }

        return mapper.writeValueAsString(response);
    }
}