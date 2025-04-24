package io.github.ritwickrajmakhal.handlers;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

public class RenameBlobHandler implements FunctionHandler {
    private final BlobClient blobClient;
    private final AzureSearchClient searchClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public RenameBlobHandler(BlobClient blobClient, AzureSearchClient searchClient) {
        this.blobClient = blobClient;
        this.searchClient = searchClient;
    }

    @Override
    public String execute(final JsonNode args) throws Exception {
        String sourceBlobName = args.has("sourceBlobName") ? args.get("sourceBlobName").asText() : "";
        String destinationBlobName = args.has("destinationBlobName") ? args.get("destinationBlobName").asText() : "";

        if (sourceBlobName.isEmpty() || destinationBlobName.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Missing required parameters: sourceBlobName and/or destinationBlobName");
            return mapper.writeValueAsString(response);
        }

        boolean success = blobClient.renameBlob(sourceBlobName, destinationBlobName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            response.put("message",
                    "Blob renamed successfully from '" + sourceBlobName + "' to '" + destinationBlobName + "'");

            // Run the indexer after successful renaming
            boolean indexerSuccess = searchClient.runIndexer();
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