package io.github.ritwickrajmakhal.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

public class SearchBlobsHandler implements FunctionHandler {
    private final AzureSearchClient searchClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public SearchBlobsHandler(AzureSearchClient searchClient) {
        this.searchClient = searchClient;
    }

    @Override
    public String execute(JsonNode args) throws Exception {
        String query = args.has("query") ? args.get("query").asText() : "";
        int maxResults = args.has("maxResults") ? args.get("maxResults").asInt() : 5;
        boolean includeContent = args.has("includeContent") ? args.get("includeContent").asBoolean() : false;

        // Execute the search
        List<Map<String, Object>> searchResults = searchClient.search(query, maxResults);

        // Format the results
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> filteredBlobs = new ArrayList<>();

        for (Map<String, Object> result : searchResults) {
            Map<String, Object> filteredBlob = new HashMap<>();
            
            // Basic info
            addIfExists(result, filteredBlob, "metadata_storage_name", "name");
            addIfExists(result, filteredBlob, "metadata_storage_path", "path");
            addIfExists(result, filteredBlob, "metadata_storage_size", "size");
            addIfExists(result, filteredBlob, "metadata_storage_last_modified", "lastModified");
            addIfExists(result, filteredBlob, "metadata_storage_content_type", "contentType");
            addIfExists(result, filteredBlob, "@search.score", "relevanceScore");
            
            // Important semantic elements
            addIfExists(result, filteredBlob, "people");
            addIfExists(result, filteredBlob, "organizations");
            addIfExists(result, filteredBlob, "locations");
            addIfExists(result, filteredBlob, "keyphrases");
            
            // Include a content summary if requested
            if (includeContent && result.containsKey("merged_content")) {
                String content = result.get("merged_content").toString();
                filteredBlob.put("contentSummary", truncateContent(content, 300));
            }
            
            // Highlight snippets if available
            if (result.containsKey("@search.captions")) {
                Object captions = result.get("@search.captions");
                if (captions instanceof List && !((List<?>) captions).isEmpty()) {
                    filteredBlob.put("highlight", captions);
                }
            }
            
            filteredBlobs.add(filteredBlob);
        }

        response.put("results", filteredBlobs);
        response.put("count", filteredBlobs.size());
        response.put("query", query);

        return mapper.writeValueAsString(response);
    }
    
    private void addIfExists(Map<String, Object> source, Map<String, Object> target, String key) {
        addIfExists(source, target, key, key);
    }
    
    private void addIfExists(Map<String, Object> source, Map<String, Object> target, 
                            String sourceKey, String targetKey) {
        if (source.containsKey(sourceKey) && source.get(sourceKey) != null) {
            target.put(targetKey, source.get(sourceKey));
        }
    }
    
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        return content.length() <= maxLength ? 
               content : content.substring(0, maxLength) + "...";
    }
}