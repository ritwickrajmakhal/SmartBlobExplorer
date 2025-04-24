package io.github.ritwickrajmakhal.handlers;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.BlobClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

public class DownloadBlobHandler implements FunctionHandler {
    private final BlobClient blobClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public DownloadBlobHandler(BlobClient blobClient) {
        this.blobClient = blobClient;
    }

    @Override
    public String execute(final JsonNode args) throws Exception {
        String blobName = args.has("blobName") ? args.get("blobName").asText() : "";
        String destinationPath = args.has("destinationPath") ? args.get("destinationPath").asText() : "";

        if (blobName.isEmpty() || destinationPath.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Missing required parameters: blobName and/or destinationPath");
            return mapper.writeValueAsString(response);
        }

        try {
            blobClient.downloadBlob(blobName, destinationPath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Blob '" + blobName + "' downloaded successfully to: " + destinationPath);
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to download blob: " + e.getMessage());
            return mapper.writeValueAsString(response);
        }
    }
}