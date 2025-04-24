package io.github.ritwickrajmakhal.handlers;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

public class UploadFileHandler implements FunctionHandler {
    private final BlobClient blobClient;
    private final AzureSearchClient searchClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public UploadFileHandler(BlobClient blobClient, AzureSearchClient searchClient) {
        this.blobClient = blobClient;
        this.searchClient = searchClient;
    }

    @Override
    public String execute(final JsonNode args) throws Exception {
        String filePath = args.has("filePath") ? args.get("filePath").asText() : "";

        if (filePath.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "No file path provided");
            return mapper.writeValueAsString(response);
        }

        boolean success = blobClient.uploadFile(filePath);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            response.put("message", "File uploaded successfully: " + filePath);

            // Run the indexer after successful upload
            boolean indexerSuccess = searchClient.runIndexer();
            if (indexerSuccess) {
                response.put("indexerRun", true);
                response.put("indexerMessage", "Search index updated successfully");
            } else {
                response.put("indexerRun", false);
                response.put("indexerMessage", "Failed to update search index");
            }
        } else {
            response.put("error", "Failed to upload file: " + filePath);
        }

        return mapper.writeValueAsString(response);
    }
}