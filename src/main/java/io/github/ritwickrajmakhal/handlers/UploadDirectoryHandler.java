package io.github.ritwickrajmakhal.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handler for uploading entire directories to blob storage.
 * <p>
 * This handler accepts a local directory path and uploads all files within it
 * to Azure Blob Storage. It can optionally:
 * <ul>
 * <li>Filter files by a pattern</li>
 * <li>Include files from subdirectories (recursive upload)</li>
 * <li>Add a prefix to all uploaded blob names</li>
 * </ul>
 * <p>
 * Expected JSON input:
 * 
 * <pre>
 * {
 *   "directoryPath": "path/to/directory", (required)
 *   "pattern": "*.pdf", (optional file filter)
 *   "recursive": true|false, (whether to include subdirectories, optional, default: false)
 *   "blobPrefix": "uploads/", (optional prefix for blob names)
 * }
 * </pre>
 * <p>
 * Response JSON:
 * 
 * <pre>
 * {
 *   "success": true|false,
 *   "totalFiles": 10, (total files found)
 *   "successCount": 8, (number of successfully uploaded files)
 *   "failedCount": 2, (number of failed uploads)
 *   "message": "Successfully uploaded 8/10 files from directory"
 * }
 * </pre>
 */
public class UploadDirectoryHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(UploadDirectoryHandler.class);

    /** Client for blob storage operations */
    private final BlobClient blobClient;

    /** Client for search operations */
    private final AzureSearchClient searchClient;

    /** JSON object mapper for serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new UploadDirectoryHandler with the specified clients.
     *
     * @param blobClient   The client for Azure Blob Storage operations
     * @param searchClient The client for Azure AI Search operations
     */
    public UploadDirectoryHandler(BlobClient blobClient, AzureSearchClient searchClient) {
        this.blobClient = blobClient;
        this.searchClient = searchClient;
    }

    /**
     * Executes the directory upload operation with the provided arguments.
     * <p>
     * This method:
     * <ol>
     * <li>Extracts directory path and optional parameters from the arguments</li>
     * <li>Uploads all matching files from the directory to blob storage</li>
     * <li>Returns a JSON response with the operation results</li>
     * </ol>
     *
     * @param args A JsonNode containing the "directoryPath" parameter and optional
     *             parameters
     * @return A JSON string containing the operation results
     * @throws Exception If there is an error during JSON serialization
     */
    @Override
    public String execute(final JsonNode args) throws Exception {
        final String directoryPath = args.has("directoryPath") ? args.get("directoryPath").asText() : "";
        final String pattern = args.has("pattern") ? args.get("pattern").asText() : null;
        final boolean recursive = args.has("recursive") && args.get("recursive").asBoolean();
        final String blobPrefix = args.has("blobPrefix") ? args.get("blobPrefix").asText() : null;

        if (directoryPath.isEmpty()) {
            logger.warn("No directory path provided for upload");
            return mapper.writeValueAsString(Map.of(
                    "success", false,
                    "error", "No directory path provided"));
        }

        logger.info("Uploading directory: {} with pattern: {}, recursive: {}, blobPrefix: {}",
                directoryPath, pattern != null ? pattern : "none",
                recursive, blobPrefix != null ? blobPrefix : "none");

        try {
            // Upload the directory using the BlobClient
            Map<String, Object> result = blobClient.uploadDirectory(
                    directoryPath, pattern, recursive, blobPrefix);

            // If search client is available, index the newly uploaded files
            if (searchClient != null && Boolean.TRUE.equals(result.get("success"))) {
                try {
                    @SuppressWarnings("unchecked")
                    var successfulUploads = (java.util.List<String>) result.get("successfulUploads");
                    if (successfulUploads != null && !successfulUploads.isEmpty()) {
                        searchClient.runIndexer();
                        logger.info("Indexed {} newly uploaded blobs", successfulUploads.size());
                    }
                } catch (Exception e) {
                    logger.error("Error indexing uploaded blobs", e);
                    // Don't fail the whole operation if indexing fails
                    result.put("indexingStatus", "Failed to index some uploaded files: " + e.getMessage());
                }
            }

            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Error uploading directory: {}", directoryPath, e);
            return mapper.writeValueAsString(Map.of(
                    "success", false,
                    "error", "Failed to upload directory: " + e.getMessage()));
        }
    }
}