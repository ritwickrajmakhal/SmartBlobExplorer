package io.github.ritwickrajmakhal.handlers;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

/**
 * Handler for renaming blobs in Azure Blob Storage and updating the search
 * index.
 * <p>
 * This handler accepts source and destination blob name parameters and:
 * <ul>
 * <li>Renames (moves) the blob by copying to the new name and deleting the
 * original</li>
 * <li>Triggers a run of the Azure Search indexer to update the index</li>
 * <li>Returns a JSON response with the results of both operations</li>
 * </ul>
 * <p>
 * Expected JSON input:
 * 
 * <pre>
 * {
 *   "sourceBlobName": "original-name.ext",
 *   "destinationBlobName": "new-name.ext"
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
 *   "indexerRun": true|false, (if rename successful)
 *   "indexerMessage": "Indexer status message" (if rename successful)
 * }
 * </pre>
 */
public class RenameBlobHandler implements FunctionHandler {
    /** Client for Azure Blob Storage operations */
    private final BlobClient blobClient;

    /** Client for Azure Search operations */
    private final AzureSearchClient searchClient;

    /** JSON object mapper for serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new RenameBlobHandler with the specified clients.
     *
     * @param blobClient   The client for Azure Blob Storage operations
     * @param searchClient The client for Azure Search operations
     */
    public RenameBlobHandler(final BlobClient blobClient, final AzureSearchClient searchClient) {
        this.blobClient = blobClient;
        this.searchClient = searchClient;
    }

    /**
     * Executes the rename blob operation with the provided arguments.
     * <p>
     * This method:
     * <ol>
     * <li>Validates that both source and destination blob names are provided</li>
     * <li>Renames the blob using copy and delete operations</li>
     * <li>If the rename is successful, triggers the Azure Search indexer</li>
     * <li>Returns a JSON response with the results of the operations</li>
     * </ol>
     * <p>
     * The underlying implementation creates a copy of the blob with the new name
     * and then deletes the original, making this effectively a move operation.
     *
     * @param args A JsonNode containing the "sourceBlobName" and
     *             "destinationBlobName" parameters
     * @return A JSON string containing the operation results
     * @throws Exception If there is an error during JSON serialization
     */
    @Override
    public String execute(final JsonNode args) throws Exception {
        final String sourceBlobName = args.has("sourceBlobName") ? args.get("sourceBlobName").asText() : "";
        final String destinationBlobName = args.has("destinationBlobName") ? args.get("destinationBlobName").asText()
                : "";

        if (sourceBlobName.isEmpty() || destinationBlobName.isEmpty()) {
            final Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Missing required parameters: sourceBlobName and/or destinationBlobName");
            return mapper.writeValueAsString(response);
        }

        final boolean success = blobClient.renameBlob(sourceBlobName, destinationBlobName);

        final Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            response.put("message",
                    "Blob renamed successfully from '" + sourceBlobName + "' to '" + destinationBlobName + "'");

            // Run the indexer after successful renaming
            final boolean indexerSuccess = searchClient.runIndexer();
            if (indexerSuccess) {
                response.put("indexerRun", true);
                response.put("indexerMessage", "Search index updated successfully");
            } else {
                response.put("indexerRun", false);
                response.put("indexerMessage", "Failed to update search index");
            }
        } else {
            response.put("error",
                    "Failed to rename blob from '" + sourceBlobName + "' to '" + destinationBlobName + "'");
        }

        return mapper.writeValueAsString(response);
    }
}