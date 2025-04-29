package io.github.ritwickrajmakhal.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handler for generating temporary shared access signature (SAS) URLs for
 * blobs.
 */
public class GenerateSasUrlHandler implements FunctionHandler {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(GenerateSasUrlHandler.class);

    /** Client for interacting with Azure Blob Storage */
    private final BlobClient blobClient;

    /** JSON object mapper for parsing arguments and serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new GenerateSasUrlHandler with the specified BlobClient.
     *
     * @param blobClient The blob client to use for SAS URL generation
     */
    public GenerateSasUrlHandler(BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    /**
     * Executes the generate SAS URL operation based on the provided arguments.
     * <p>
     * Expected arguments:
     * <ul>
     * <li>blobName: Name of the blob to generate a SAS URL for</li>
     * <li>durationHours: Number of hours the SAS URL should be valid (default:
     * 24)</li>
     * <li>readPermission: Whether the SAS should allow read access (default:
     * true)</li>
     * <li>writePermission: Whether the SAS should allow to write access (default:
     * false)</li>
     * <li>deletePermission: Whether the SAS should allow to delete access (default:
     * false)</li>
     * </ul>
     *
     * @param args The arguments provided by the OpenAI model
     * @return A JSON string with the generated SAS URL
     * @throws Exception If an error occurs during execution
     */
    @Override
    public String execute(JsonNode args) throws Exception {
        try {
            logger.info("Executing generate_sas_url with args: {}", args);

            // Extract arguments with default values
            String blobName = args.has("blobName") ? args.get("blobName").asText() : null;
            int durationHours = args.has("durationHours") ? args.get("durationHours").asInt() : 24;
            boolean readPermission = !args.has("readPermission") || args.get("readPermission").asBoolean();
            boolean writePermission = args.has("writePermission") && args.get("writePermission").asBoolean();
            boolean deletePermission = args.has("deletePermission") && args.get("deletePermission").asBoolean();

            // Validate arguments
            if (blobName == null || blobName.isEmpty()) {
                return mapper.writeValueAsString(Map.of(
                        "error", "Missing or invalid 'blobName' argument. Please specify a valid blob name."));
            }

            if (durationHours <= 0 || durationHours > 168) { // Max 7 days (168 hours)
                return mapper.writeValueAsString(Map.of(
                        "error", "Invalid 'durationHours' value. Duration must be between 1 and 168 hours."));
            }

            // Execute SAS URL generation
            String sasUrl = blobClient.generateSasUrl(blobName, durationHours, readPermission, writePermission,
                    deletePermission);

            // Create response
            if (sasUrl != null) {
                return mapper.writeValueAsString(Map.of(
                        "message", String.format("Successfully generated SAS URL for blob '%s', valid for %d hours",
                                blobName, durationHours),
                        "blobName", blobName,
                        "durationHours", durationHours,
                        "permissions", Map.of(
                                "read", readPermission,
                                "write", writePermission,
                                "delete", deletePermission),
                        "sasUrl", sasUrl));
            } else {
                return mapper.writeValueAsString(Map.of(
                        "error", String.format("Failed to generate SAS URL for blob '%s'", blobName)));
            }
        } catch (Exception e) {
            logger.error("Error in generate_sas_url operation", e);
            return mapper.writeValueAsString(Map.of(
                    "error", "Failed to generate SAS URL: " + e.getMessage()));
        }
    }
}