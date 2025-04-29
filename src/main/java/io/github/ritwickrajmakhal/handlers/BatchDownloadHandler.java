package io.github.ritwickrajmakhal.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler for downloading multiple blobs to local files.
 */
public class BatchDownloadHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(BatchDownloadHandler.class);

    /** Client for interacting with Azure Blob Storage */
    private final BlobClient blobClient;

    /** JSON object mapper for parsing arguments and serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new BatchDownloadHandler with the specified BlobClient.
     *
     * @param blobClient The blob client to use for batch download operations
     */
    public BatchDownloadHandler(BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    /**
     * Executes the batch download operation based on the provided arguments.
     * <p>
     * Expected arguments:
     * <ul>
     * <li>blobNames: Array of blob names to download</li>
     * <li>destinationDir: Directory to save the downloaded files</li>
     * </ul>
     *
     * @param args The arguments provided by the OpenAI model
     * @return A JSON string with the list of successfully downloaded blobs
     * @throws Exception If an error occurs during execution
     */
    @Override
    public String execute(JsonNode args) throws Exception {
        try {
            logger.info("Executing batch_download with args: {}", args);

            // Extract arguments
            JsonNode blobNamesNode = args.get("blobNames");
            if (blobNamesNode == null || !blobNamesNode.isArray() || blobNamesNode.isEmpty()) {
                return mapper.writeValueAsString(Map.of(
                        "error", "Missing or invalid 'blobNames' argument. Expected a non-empty array of blob names."));
            }

            String destinationDir = args.has("destinationDir") ? args.get("destinationDir").asText() : null;
            if (destinationDir == null || destinationDir.isEmpty()) {
                return mapper.writeValueAsString(Map.of(
                        "error", "Missing or invalid 'destinationDir' argument. Expected a valid directory path."));
            }

            // Convert JSON array to list of strings
            List<String> blobNames = new ArrayList<>();
            for (JsonNode name : blobNamesNode) {
                blobNames.add(name.asText());
            }

            // Execute batch download
            List<String> downloadedBlobs = blobClient.batchDownloadBlobs(blobNames, destinationDir);

            // Create response
            ObjectNode response = mapper.createObjectNode();
            response.put("message", String.format("Successfully downloaded %d/%d blobs to %s",
                    downloadedBlobs.size(), blobNames.size(), destinationDir));

            ArrayNode downloadedBlobsNode = mapper.createArrayNode();
            downloadedBlobs.forEach(downloadedBlobsNode::add);
            response.set("downloadedBlobs", downloadedBlobsNode);

            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error in batch_download operation", e);
            return mapper.writeValueAsString(Map.of(
                    "error", "Failed to perform batch download: " + e.getMessage()));
        }
    }
}