package io.github.ritwickrajmakhal.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Handler for listing blobs in Azure Storage with filtering options.
 */
public class ListBlobsHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(ListBlobsHandler.class);

    /** Client for interacting with Azure Blob Storage */
    private final BlobClient blobClient;

    /** JSON object mapper for parsing arguments and serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new ListBlobsHandler with the specified BlobClient.
     *
     * @param blobClient The blob client to use for listing blobs
     */
    public ListBlobsHandler(BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    /**
     * Executes the list blobs operation based on the provided arguments.
     * <p>
     * Expected arguments:
     * <ul>
     * <li>prefix: (optional) Prefix to filter blob names</li>
     * <li>regex: (optional) Regular expression to filter blob names</li>
     * <li>maxResults: (optional) Maximum number of results to return</li>
     * </ul>
     *
     * @param args The arguments provided by the OpenAI model
     * @return A JSON string with the list of blobs matching the filter criteria
     * @throws Exception If an error occurs during execution
     */
    @Override
    public String execute(JsonNode args) throws Exception {
        try {
            logger.info("Executing list_blobs with args: {}", args);

            // Extract arguments with default values
            String prefix = args.has("prefix") ? args.get("prefix").asText() : null;
            String regex = args.has("regex") ? args.get("regex").asText() : null;
            int maxResults = args.has("maxResults") ? args.get("maxResults").asInt() : 0;

            // Execute list operation
            List<Map<String, Object>> blobs = blobClient.listBlobs(prefix, regex, maxResults);

            // Create response
            ObjectNode response = mapper.createObjectNode();
            response.put("message", String.format("Found %d blob(s)", blobs.size()));

            ArrayNode blobsNode = mapper.createArrayNode();
            for (Map<String, Object> blob : blobs) {
                ObjectNode blobNode = mapper.createObjectNode();

                // Add basic blob properties
                blobNode.put("name", (String) blob.get("name"));
                blobNode.put("size", (Long) blob.get("size"));
                blobNode.put("contentType", (String) blob.get("contentType"));
                blobNode.put("lastModified", blob.get("lastModified").toString());

                // Add metadata if available
                @SuppressWarnings("unchecked")
                Map<String, String> metadata = (Map<String, String>) blob.get("metadata");
                if (metadata != null && !metadata.isEmpty()) {
                    ObjectNode metadataNode = mapper.createObjectNode();
                    for (Map.Entry<String, String> entry : metadata.entrySet()) {
                        metadataNode.put(entry.getKey(), entry.getValue());
                    }
                    blobNode.set("metadata", metadataNode);
                }

                blobsNode.add(blobNode);
            }
            response.set("blobs", blobsNode);

            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error in list_blobs operation", e);
            return mapper.writeValueAsString(Map.of(
                    "error", "Failed to list blobs: " + e.getMessage()));
        }
    }
}