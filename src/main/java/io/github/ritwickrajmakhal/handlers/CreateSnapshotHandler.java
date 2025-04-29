package io.github.ritwickrajmakhal.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Handler for creating point-in-time snapshots of blobs for version control.
 */
public class CreateSnapshotHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(CreateSnapshotHandler.class);

    /** Client for interacting with Azure Blob Storage */
    private final BlobClient blobClient;

    /** JSON object mapper for parsing arguments and serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new CreateSnapshotHandler with the specified BlobClient.
     *
     * @param blobClient The blob client to use for snapshot operations
     */
    public CreateSnapshotHandler(BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    /**
     * Executes the create snapshot operation based on the provided arguments.
     * <p>
     * Expected arguments:
     * <ul>
     * <li>blobName: Name of the blob to create a snapshot of</li>
     * </ul>
     *
     * @param args The arguments provided by the OpenAI model
     * @return A JSON string with the result of the snapshot operation
     * @throws Exception If an error occurs during execution
     */
    @Override
    public String execute(JsonNode args) throws Exception {
        try {
            logger.info("Executing create_snapshot with args: {}", args);

            // Extract arguments
            String blobName = args.has("blobName") ? args.get("blobName").asText() : null;

            // Validate arguments
            if (blobName == null || blobName.isEmpty()) {
                return mapper.writeValueAsString(Map.of(
                        "error", "Missing or invalid 'blobName' argument. Please specify a valid blob name."));
            }

            // Execute snapshot creation
            Map<String, Object> snapshotInfo = blobClient.createSnapshot(blobName);

            // Create response
            return mapper.writeValueAsString(Objects.requireNonNullElseGet(snapshotInfo, () -> Map.of(
                    "error", String.format("Failed to create snapshot of blob '%s'", blobName))));
        } catch (Exception e) {
            logger.error("Error in create_snapshot operation", e);
            return mapper.writeValueAsString(Map.of(
                    "error", "Failed to create snapshot: " + e.getMessage()));
        }
    }
}