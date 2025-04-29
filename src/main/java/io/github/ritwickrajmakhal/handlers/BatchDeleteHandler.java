package io.github.ritwickrajmakhal.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for deleting multiple blobs from storage.
 */
public class BatchDeleteHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(BatchDeleteHandler.class);

    /** Client for interacting with Azure Blob Storage */
    private final BlobClient blobClient;

    /** Client for interacting with Azure Search */
    private final AzureSearchClient azureSearchClient;

    /** JSON object mapper for parsing arguments and serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new BatchDeleteHandler with the specified BlobClient.
     *
     * @param blobClient        The blob client to use for batch delete operations
     * @param azureSearchClient The Azure Search client to use for search operations
     */
    public BatchDeleteHandler(BlobClient blobClient, AzureSearchClient azureSearchClient) {
        this.blobClient = blobClient;
        this.azureSearchClient = azureSearchClient;
    }

    /**
     * Executes the batch delete operation based on the provided arguments.
     * <p>
     * Expected arguments:
     * <ul>
     * <li>blobNames: Array of blob names to delete</li>
     * </ul>
     *
     * @param args The arguments provided by the OpenAI model
     * @return A JSON string with the list of successfully deleted blobs and any
     *         failed operations
     * @throws Exception If an error occurs during execution
     */
    @Override
    public String execute(JsonNode args) throws Exception {
        logger.info("Executing batch_delete with args: {}", args);
        List<String> deletedBlobs = new ArrayList<>();
        List<String> failedBlobs = new ArrayList<>();
        boolean indexerRunSuccess = true;

        try {
            // Extract arguments
            JsonNode blobNamesNode = args.get("blobNames");
            if (blobNamesNode == null || !blobNamesNode.isArray() || blobNamesNode.isEmpty()) {
                return mapper.writeValueAsString(Map.of(
                        "error", "Missing or invalid 'blobNames' argument. Expected a non-empty array of blob names."));
            }

            // Convert JSON array to list of strings
            List<String> blobNames = new ArrayList<>();
            for (JsonNode name : blobNamesNode) {
                blobNames.add(name.asText());
            }

            // Execute batch delete
            deletedBlobs = blobClient.batchDeleteBlobs(blobNames);

            // Calculate which blobs failed
            for (String blobName : blobNames) {
                if (!deletedBlobs.contains(blobName)) {
                    failedBlobs.add(blobName);
                }
            }

            // Update Azure Search index if necessary and track success/failure
            if (!deletedBlobs.isEmpty()) {
                try {
                    indexerRunSuccess = azureSearchClient.runIndexer();
                } catch (Exception e) {
                    indexerRunSuccess = false;
                    logger.error("Failed to update search index after blob deletion", e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in batch_delete operation", e);
            return mapper.writeValueAsString(Map.of(
                    "error", "Failed to perform batch delete: " + e.getMessage()));
        }

        try {
            // Create detailed response including success and failure information
            ObjectNode response = mapper.createObjectNode();

            // Add summary message
            String summaryMessage;
            if (failedBlobs.isEmpty()) {
                summaryMessage = String.format("Successfully deleted all %d blobs", deletedBlobs.size());
            } else {
                summaryMessage = String.format("Deleted %d/%d blobs. %d operations failed",
                        deletedBlobs.size(), deletedBlobs.size() + failedBlobs.size(), failedBlobs.size());
            }

            if (!indexerRunSuccess && !deletedBlobs.isEmpty()) {
                summaryMessage += ". Warning: Search index update failed.";
            }

            response.put("message", summaryMessage);
            response.put("success", !deletedBlobs.isEmpty());
            response.put("indexUpdated", indexerRunSuccess);

            // Add successful deletions
            ArrayNode deletedBlobsNode = mapper.createArrayNode();
            deletedBlobs.forEach(deletedBlobsNode::add);
            response.set("deletedBlobs", deletedBlobsNode);

            // Add failures if any
            if (!failedBlobs.isEmpty()) {
                ArrayNode failedBlobsNode = mapper.createArrayNode();
                failedBlobs.forEach(failedBlobsNode::add);
                response.set("failedBlobs", failedBlobsNode);
            }

            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            // Fallback response if JSON serialization fails
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "Failed to format response: " + e.getMessage());
            fallback.put("deletedBlobsCount", deletedBlobs.size());
            return mapper.writeValueAsString(fallback);
        }
    }
}