package io.github.ritwickrajmakhal.handlers;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

/**
 * Handler for deleting blobs from Azure Blob Storage and updating the search
 * index.
 * <p>
 * This handler accepts a blob name parameter and:
 * <ul>
 * <li>Deletes the specified blob from Azure Blob Storage</li>
 * <li>Triggers a run of the Azure Search indexer to update the index</li>
 * <li>Returns a JSON response with the results of both operations</li>
 * </ul>
 * <p>
 * Expected JSON input:
 * 
 * <pre>
 * {
 *   "blobName": "name-of-blob-to-delete.ext"
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
 *   "indexerRun": true|false, (if deletion successful)
 *   "indexerMessage": "Indexer status message" (if deletion successful)
 * }
 * </pre>
 */
public class DeleteBlobHandler implements FunctionHandler {
    /** Client for Azure Blob Storage operations */
    private final BlobClient blobClient;

    /** Client for Azure Search operations */
    private final AzureSearchClient searchClient;

    /** JSON object mapper for serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new DeleteBlobHandler with the specified clients.
     *
     * @param blobClient   The client for Azure Blob Storage operations
     * @param searchClient The client for Azure Search operations
     */
    public DeleteBlobHandler(final BlobClient blobClient, final AzureSearchClient searchClient) {
        this.blobClient = blobClient;
        this.searchClient = searchClient;
    }

    /**
     * Executes the delete blob operation with the provided arguments.
     * <p>
     * This method:
     * <ol>
     * <li>Validates that a blob name is provided</li>
     * <li>Deletes the specified blob from Azure Blob Storage</li>
     * <li>If the deletion is successful, triggers the Azure Search indexer to
     * update the index</li>
     * <li>Returns a JSON response with the results of the operations</li>
     * </ol>
     *
     * @param args A JsonNode containing the "blobName" parameter
     * @return A JSON string containing the operation results
     * @throws Exception If there is an error during JSON serialization
     */
    @Override
    public String execute(final JsonNode args) throws Exception {
        final String blobName = args.has("blobName") ? args.get("blobName").asText() : "";

        if (blobName.isEmpty()) {
            final Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "No blob name provided");
            return mapper.writeValueAsString(response);
        }

        final boolean success = blobClient.deleteBlob(blobName);

        final Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            response.put("message", "Blob deleted successfully: " + blobName);

            // Run the indexer after successful deletion
            final boolean indexerSuccess = searchClient.runIndexer();
            if (indexerSuccess) {
                response.put("indexerRun", true);
                response.put("indexerMessage", "Search index updated successfully");
            } else {
                response.put("indexerRun", false);
                response.put("indexerMessage", "Failed to update search index");
            }
        } else {
            response.put("error", "Failed to delete blob: " + blobName);
        }

        return mapper.writeValueAsString(response);
    }
}