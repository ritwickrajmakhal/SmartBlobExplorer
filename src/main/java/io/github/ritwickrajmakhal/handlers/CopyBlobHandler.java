package io.github.ritwickrajmakhal.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handler for copying a blob while preserving the original.
 */
public class CopyBlobHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(CopyBlobHandler.class);

    /** Client for interacting with Azure Blob Storage */
    private final BlobClient blobClient;

    /** JSON object mapper for parsing arguments and serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new CopyBlobHandler with the specified BlobClient.
     *
     * @param blobClient The blob client to use for blob copy operations
     */
    public CopyBlobHandler(BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    /**
     * Executes the copy blob operation based on the provided arguments.
     * <p>
     * Expected arguments:
     * <ul>
     * <li>sourceBlobName: Name of the source blob to copy</li>
     * <li>destinationBlobName: New name for the copied blob</li>
     * </ul>
     *
     * @param args The arguments provided by the OpenAI model
     * @return A JSON string with the result of the copy operation
     * @throws Exception If an error occurs during execution
     */
    @Override
    public String execute(JsonNode args) throws Exception {
        try {
            logger.info("Executing copy_blob with args: {}", args);

            // Extract arguments
            String sourceBlobName = args.has("sourceBlobName") ? args.get("sourceBlobName").asText() : null;
            String destinationBlobName = args.has("destinationBlobName") ? args.get("destinationBlobName").asText()
                    : null;

            // Validate arguments
            if (sourceBlobName == null || sourceBlobName.isEmpty()) {
                return mapper.writeValueAsString(Map.of(
                        "error", "Missing or invalid 'sourceBlobName' argument. Please specify a valid blob name."));
            }
            if (destinationBlobName == null || destinationBlobName.isEmpty()) {
                return mapper.writeValueAsString(Map.of(
                        "error",
                        "Missing or invalid 'destinationBlobName' argument. Please specify a valid destination blob name."));
            }

            // Execute copy operation
            boolean success = blobClient.copyBlob(sourceBlobName, destinationBlobName);

            // Create response
            if (success) {
                return mapper.writeValueAsString(Map.of(
                        "message",
                        String.format("Successfully copied blob from '%s' to '%s'", sourceBlobName,
                                destinationBlobName),
                        "sourceBlobName", sourceBlobName,
                        "destinationBlobName", destinationBlobName));
            } else {
                return mapper.writeValueAsString(Map.of(
                        "error",
                        String.format("Failed to copy blob from '%s' to '%s'", sourceBlobName, destinationBlobName)));
            }
        } catch (Exception e) {
            logger.error("Error in copy_blob operation", e);
            return mapper.writeValueAsString(Map.of(
                    "error", "Failed to copy blob: " + e.getMessage()));
        }
    }
}