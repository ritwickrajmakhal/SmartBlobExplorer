package io.github.ritwickrajmakhal.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ritwickrajmakhal.AzureSearchClient;
import io.github.ritwickrajmakhal.interfaces.FunctionHandler;

/**
 * Handler for searching blobs via the Azure AI Search index.
 * <p>
 * This handler accepts search parameters and:
 * <ul>
 * <li>Executes a search query against the Azure AI Search index</li>
 * <li>Filters and formats the search results to include relevant metadata</li>
 * <li>Optionally includes content summaries from the documents</li>
 * <li>Returns a JSON response with the formatted search results</li>
 * </ul>
 * <p>
 * Expected JSON input:
 * 
 * <pre>
 * {
 *   "query": "search terms",
 *   "maxResults": 5, (optional, default is 5)
 *   "includeContent": false (optional, default is false)
 * }
 * </pre>
 * <p>
 * Response JSON:
 * 
 * <pre>
 * {
 *   "results": [
 *     {
 *       "name": "filename.ext",
 *       "path": "blob/path",
 *       "size": 12345,
 *       "lastModified": "2023-01-01T12:00:00Z",
 *       "contentType": "application/pdf",
 *       "relevanceScore": 0.95,
 *       "people": ["Person1", "Person2"],
 *       "organizations": ["Org1", "Org2"],
 *       "locations": ["Location1"],
 *       "keyphrases": ["key phrase 1", "key phrase 2"],
 *       "contentSummary": "Document content preview..." (if includeContent is true),
 *       "highlight": [...] (if available)
 *     },
 *     ...
 *   ],
 *   "count": 3,
 *   "query": "original search query"
 * }
 * </pre>
 */
public class SearchBlobsHandler implements FunctionHandler {
    /** Client for Azure AI Search operations */
    private final AzureSearchClient searchClient;

    /** JSON object mapper for serializing responses */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new SearchBlobsHandler with the specified search client.
     *
     * @param searchClient The client for Azure AI Search operations
     */
    public SearchBlobsHandler(final AzureSearchClient searchClient) {
        this.searchClient = searchClient;
    }

    /**
     * Executes a search operation against the Azure AI Search index with the
     * provided arguments.
     * <p>
     * This method:
     * <ol>
     * <li>Parses search query, maximum results, and content inclusion
     * parameters</li>
     * <li>Performs the search using the search client</li>
     * <li>Processes and formats the search results for presentation</li>
     * <li>Returns a JSON response with the formatted search results</li>
     * </ol>
     * <p>
     * The search results include metadata about the matching blobs, extracted
     * entities,
     * key phrases, and optionally a summary of the document content.
     *
     * @param args A JsonNode containing the search parameters
     * @return A JSON string containing the search results
     * @throws Exception If there is an error during search or JSON serialization
     */
    @Override
    public String execute(final JsonNode args) throws Exception {
        final String query = args.has("query") ? args.get("query").asText() : "";
        final int maxResults = args.has("maxResults") ? args.get("maxResults").asInt() : 5;
        final boolean includeContent = args.has("includeContent") && args.get("includeContent").asBoolean();

        // Execute the search
        final List<Map<String, Object>> searchResults = searchClient.search(query, maxResults);

        // Format the results
        final Map<String, Object> response = new HashMap<>();
        final List<Map<String, Object>> filteredBlobs = new ArrayList<>();

        for (Map<String, Object> result : searchResults) {
            final Map<String, Object> filteredBlob = new HashMap<>();

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
                final String content = result.get("merged_content").toString();
                filteredBlob.put("contentSummary", truncateContent(content));
            }

            // Highlight snippets if available
            if (result.containsKey("@search.captions")) {
                final Object captions = result.get("@search.captions");
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

    /**
     * Adds a field from the source map to the target map using the same key name.
     * <p>
     * This is a convenience method that calls
     * {@link #addIfExists(Map, Map, String, String)}
     * with the same key for both source and target.
     *
     * @param source The source map containing the original field
     * @param target The target map where the field should be added
     * @param key    The key to use for both source and target
     */
    private void addIfExists(final Map<String, Object> source, final Map<String, Object> target, final String key) {
        addIfExists(source, target, key, key);
    }

    /**
     * Adds a field from the source map to the target map if it exists and is not
     * null.
     * <p>
     * This method allows mapping fields from the search result to the filtered
     * output
     * with different key names as needed.
     *
     * @param source    The source map containing the original field
     * @param target    The target map where the field should be added
     * @param sourceKey The key in the source map
     * @param targetKey The key to use in the target map
     */
    private void addIfExists(final Map<String, Object> source, final Map<String, Object> target, final String sourceKey,
            String targetKey) {
        if (source.containsKey(sourceKey) && source.get(sourceKey) != null) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    /**
     * Truncates content to a specified maximum length and adds an ellipsis if
     * truncated.
     * <p>
     * This method is used to create summaries of document content that aren't too
     * long
     * for display purposes.
     *
     * @param content The content string to truncate
     * @return The truncated string, possibly with an ellipsis appended
     */
    private String truncateContent(final String content) {
        if (content == null)
            return "";
        return content.length() <= 300 ? content : content.substring(0, 300) + "...";
    }
}