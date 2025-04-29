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
 * Handler for uploading multiple files to blob storage.
 */
public class BatchUploadHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(BatchUploadHandler.class);

    /** Client for interacting with Azure Blob Storage */
    private final BlobClient blobClient;

    /** Client for interacting with Azure Search */
    private final AzureSearchClient azureSearchClient;

    /** JSON object mapper for parsing arguments and serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new BatchUploadHandler with the specified BlobClient and
     * AzureSearchClient.
     *
     * @param blobClient        The blob client to use for batch upload operations
     * @param azureSearchClient The Azure Search client to use for search operations
     */
    public BatchUploadHandler(BlobClient blobClient, AzureSearchClient azureSearchClient) {
        this.blobClient = blobClient;
        this.azureSearchClient = azureSearchClient;
    }

    /**
     * Executes the batch upload operation based on the provided arguments.
     * <p>
     * Expected arguments:
     * <ul>
     * <li>filePaths: Array of file paths or URLs to upload</li>
     * </ul>
     *
     * @param args The arguments provided by the OpenAI model
     * @return A JSON string with the list of successfully uploaded blobs and any
     *         failed operations
     * @throws Exception If an error occurs during execution
     */
    @Override
    public String execute(JsonNode args) throws Exception {
        logger.info("Executing batch_upload with args: {}", args);
        List<String> uploadedBlobs = new ArrayList<>();
        List<String> failedUploads = new ArrayList<>();
        boolean indexerRunSuccess = true;

        try {
            // Extract arguments
            JsonNode filePathsNode = args.get("filePaths");
            if (filePathsNode == null || !filePathsNode.isArray() || filePathsNode.isEmpty()) {
                return mapper.writeValueAsString(Map.of(
                        "error", "Missing or invalid 'filePaths' argument. Expected a non-empty array of file paths."));
            }

            // Convert JSON array to list of strings
            List<String> filePaths = new ArrayList<>();
            for (JsonNode path : filePathsNode) {
                filePaths.add(path.asText());
            }

            // Execute batch upload
            uploadedBlobs = blobClient.batchUploadFiles(filePaths);

            // Identify failed uploads
            for (String filePath : filePaths) {
                String fileName = filePath;

                // Extract just the filename for comparison
                if (filePath.contains("/")) {
                    fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                } else if (filePath.contains("\\")) {
                    fileName = filePath.substring(filePath.lastIndexOf('\\') + 1);
                }

                // Check if this file was successfully uploaded
                boolean uploaded = false;
                for (String uploadedBlob : uploadedBlobs) {
                    if (uploadedBlob.equals(fileName)) {
                        uploaded = true;
                        break;
                    }
                }

                if (!uploaded) {
                    failedUploads.add(filePath);
                }
            }

            // Update Azure Search index if we uploaded anything
            if (!uploadedBlobs.isEmpty()) {
                try {
                    indexerRunSuccess = azureSearchClient.runIndexer();
                } catch (Exception e) {
                    indexerRunSuccess = false;
                    logger.error("Failed to update search index after blob upload", e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in batch_upload operation", e);
            return mapper.writeValueAsString(Map.of(
                    "error", "Failed to perform batch upload: " + e.getMessage()));
        }

        try {
            // Create detailed response
            ObjectNode response = mapper.createObjectNode();

            // Add summary message
            String summaryMessage;
            if (failedUploads.isEmpty()) {
                summaryMessage = String.format("Successfully uploaded all %d files", uploadedBlobs.size());
            } else {
                summaryMessage = String.format("Uploaded %d/%d files. %d operations failed",
                        uploadedBlobs.size(), uploadedBlobs.size() + failedUploads.size(), failedUploads.size());
            }

            if (!indexerRunSuccess && !uploadedBlobs.isEmpty()) {
                summaryMessage += ". Warning: Search index update failed.";
            }

            response.put("message", summaryMessage);
            response.put("success", !uploadedBlobs.isEmpty());
            response.put("indexUpdated", indexerRunSuccess);

            // Add successful uploads
            ArrayNode uploadedBlobsNode = mapper.createArrayNode();
            uploadedBlobs.forEach(uploadedBlobsNode::add);
            response.set("uploadedBlobs", uploadedBlobsNode);

            // Add failures if any
            if (!failedUploads.isEmpty()) {
                ArrayNode failedUploadsNode = mapper.createArrayNode();
                failedUploads.forEach(failedUploadsNode::add);
                response.set("failedUploads", failedUploadsNode);
            }

            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            // Fallback response if JSON serialization fails
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "Failed to format response: " + e.getMessage());
            fallback.put("uploadedBlobsCount", uploadedBlobs.size());
            return mapper.writeValueAsString(fallback);
        }
    }
}