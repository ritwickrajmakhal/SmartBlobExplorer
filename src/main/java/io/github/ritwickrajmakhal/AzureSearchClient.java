package io.github.ritwickrajmakhal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Context;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.search.documents.indexes.SearchIndexerClient;
import com.azure.search.documents.indexes.SearchIndexerClientBuilder;
import com.azure.search.documents.indexes.models.SearchField;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.indexes.models.SearchIndexer;
import com.azure.search.documents.indexes.models.SearchIndexerDataContainer;
import com.azure.search.documents.indexes.models.SearchIndexerDataSourceConnection;
import com.azure.search.documents.indexes.models.SearchIndexerDataSourceType;
import com.azure.search.documents.indexes.models.SearchIndexerSkill;
import com.azure.search.documents.indexes.models.SearchIndexerSkillset;
import com.azure.search.documents.models.SearchOptions;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.ritwickrajmakhal.config.SearchIndexConfig;
import io.github.ritwickrajmakhal.config.SearchIndexerConfig;
import io.github.ritwickrajmakhal.config.SkillsetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for Azure AI Search service that provides functionality to create and
 * manage
 * search resources for blob storage content.
 * <p>
 * This class handles the complete lifecycle of Azure Search resources
 * including:
 * <ul>
 * <li>Creating data sources connected to Azure Blob Storage</li>
 * <li>Creating skillsets with cognitive skills for content processing</li>
 * <li>Creating search indexes with appropriate fields and suggesters</li>
 * <li>Creating and running indexers to populate the search index</li>
 * <li>Performing searches against the created index</li>
 * <li>Cleaning up all created resources</li>
 * </ul>
 * <p>
 * The class uses environment variables for Azure AI Search credentials and
 * generates
 * unique identifiers for all created resources.
 */
public class AzureSearchClient {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(AzureSearchClient.class);

    /** Environment variable loader */
    private final static Dotenv dotenv = Dotenv.load();

    /** Unique identifier for all resources created by this instance */
    private final static String uuid = UUID.randomUUID().toString();

    /** Azure AI Search endpoint URL from environment variables */
    private static final String SEARCH_ENDPOINT = dotenv.get("AZURE_AI_SEARCH_ENDPOINT");

    /** Azure AI Search API key from environment variables */
    private static final String SEARCH_API_KEY = dotenv.get("AZURE_AI_SEARCH_API_KEY");

    /** Connection string for Azure Blob Storage */
    private final String blobStorageConnectionString;

    /** Name of the blob container to index */
    private final String containerName;

    /** Optional folder path within the container (can be empty) */
    private final String folder;

    // resources
    /** Data source connection to Azure Blob Storage */
    private final SearchIndexerDataSourceConnection dataSource;

    /** Search index definition */
    private final SearchIndex index;

    /** Skillset with cognitive skills for processing content */
    private final SearchIndexerSkillset skillset;

    /** Search indexer that maps data from source to index */
    private final SearchIndexer indexer;

    // clients
    /** Client for managing indexers, data sources, and skillsets */
    private final SearchIndexerClient indexerClient;

    /** Client for managing search indexes */
    private final SearchIndexClient indexClient;

    /** Client for performing search queries */
    private final SearchClient searchClient;

    /**
     * Creates a new AzureSearchClient and initializes all required search
     * resources.
     * <p>
     * This constructor performs the following operations:
     * <ol>
     * <li>Creates Azure Search clients (indexer, index, search)</li>
     * <li>Creates a data source connected to the specified blob container</li>
     * <li>Creates a skillset with default cognitive skills</li>
     * <li>Creates a search index with default fields</li>
     * <li>Creates an indexer that connects the data source to the index using the
     * skillset</li>
     * </ol>
     *
     * @param blobStorageConnectionString The connection string for the Azure Blob
     *                                    Storage account
     * @param containerName               The name of the blob container to index
     * @param folder                      Optional folder path within the container
     *                                    to limit indexing scope (can be empty to
     *                                    index the entire container)
     */
    public AzureSearchClient(final String blobStorageConnectionString, final String containerName,
            final String folder) {
        this.blobStorageConnectionString = blobStorageConnectionString;
        this.containerName = containerName;
        this.folder = folder;

        // Create search indexer client
        indexerClient = new SearchIndexerClientBuilder()
                .endpoint(SEARCH_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .buildClient();
        // Create search index client
        indexClient = new SearchIndexClientBuilder()
                .endpoint(SEARCH_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .buildClient();

        // Create a data source for blob storage
        dataSource = createBlobDataSource(indexerClient);

        // Create a skillsset
        skillset = createSkillset(indexerClient);

        // Create an index
        index = createIndex(indexClient);

        // Create an indexer that uses the skillset and data source and loads the index
        indexer = createSearchIndexer(indexerClient, dataSource, index, skillset);

        searchClient = new SearchClientBuilder()
                .endpoint(SEARCH_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .indexName(index.getName())
                .buildClient();

        logger.info("AzureSearchClient initialized successfully");
    }

    /**
     * Creates a skillset with cognitive skills for processing content.
     * <p>
     * The skillset includes default skills defined in {@link SkillsetConfig} and
     * associates cognitive services for operations that require AI capabilities.
     *
     * @param client The search indexer client to use
     * @return The created or updated skillset
     */
    private static SearchIndexerSkillset createSkillset(final SearchIndexerClient client) {
        List<SearchIndexerSkill> skills = SkillsetConfig.getDefaultSkills();
        SearchIndexerSkillset skillset = new SearchIndexerSkillset("skillset-" + uuid, skills)
                .setDescription("Skillset for testing default configuration")
                .setCognitiveServicesAccount(SkillsetConfig.getCognitiveServicesConfig());

        return client.createOrUpdateSkillset(skillset);
    }

    /**
     * Creates a search index with fields for storing and searching document
     * content.
     * <p>
     * The index includes default fields defined in {@link SearchIndexConfig} and
     * configures suggesters for autocomplete functionality.
     *
     * @param client The search index client to use
     * @return The created or updated search index
     */
    private static SearchIndex createIndex(final SearchIndexClient client) {
        List<SearchField> fields = SearchIndexConfig.getDefaultFields();
        // Index definition
        SearchIndex index = new SearchIndex("index-" + uuid, fields);

        // Set Suggester
        index.setSuggesters(SearchIndexConfig.getDefaultSuggester());

        return client.createOrUpdateIndex(index);
    }

    /**
     * Creates a search indexer that connects a data source to an index using a
     * skillset.
     * <p>
     * The indexer manages the process of extracting data from the source, applying
     * transformations via skills, and loading the results into the index.
     *
     * @param indexerClient The search indexer client to use
     * @param dataSource    The data source connection to use
     * @param index         The search index to populate
     * @param skillset      The skillset to use for content processing
     * @return The created or updated search indexer
     */
    private static SearchIndexer createSearchIndexer(
            final SearchIndexerClient indexerClient,
            final SearchIndexerDataSourceConnection dataSource,
            final SearchIndex index,
            final SearchIndexerSkillset skillset) {

        // Use the SearchIndexerConfig to create and configure the indexer
        String indexerName = "indexer-" + uuid;
        return SearchIndexerConfig.createIndexer(
                indexerClient,
                indexerName,
                dataSource.getName(),
                index.getName(),
                skillset.getName());
    }

    /**
     * Creates a data source connection to use with an Azure Search indexer.
     * <p>
     * The data source defines the connection to an external data store such as
     * blob storage, and optionally includes change detection policies.
     *
     * @param client           The search indexer client to use
     * @param connectionString The connection string for the data source
     * @param container        The container information for the data
     *                         source
     * @return The created or updated data source connection
     */
    private static SearchIndexerDataSourceConnection createDataSource(
            final SearchIndexerClient client,
            final String connectionString,
            final SearchIndexerDataContainer container) {

        String datasourceName = "datasource-" + uuid;
        SearchIndexerDataSourceConnection dataSource = new SearchIndexerDataSourceConnection(
                datasourceName,
                SearchIndexerDataSourceType.AZURE_BLOB,
                connectionString,
                container)
                .setDataChangeDetectionPolicy(null);

        try {
            return client.createOrUpdateDataSourceConnection(dataSource);
        } catch (Exception e) {
            logger.error("Error creating data source: {}", datasourceName, e);
        }
        return dataSource;
    }

    /**
     * Creates a data source connection specifically for Azure Blob Storage.
     * <p>
     * If a folder path is specified, the data source will be limited to that
     * folder;
     * otherwise, the entire container will be indexed.
     *
     * @param client The search indexer client to use
     * @return The created or updated blob data source connection
     */
    private SearchIndexerDataSourceConnection createBlobDataSource(final SearchIndexerClient client) {
        SearchIndexerDataContainer dataContainer;
        if (folder == "") {
            dataContainer = new SearchIndexerDataContainer(containerName);
        } else {
            dataContainer = new SearchIndexerDataContainer(containerName).setQuery(folder);
        }
        return createDataSource(
                client,
                this.blobStorageConnectionString,
                dataContainer
        );
    }

    /**
     * Performs a search against the index with the given query string.
     * <p>
     * This method executes a search operation against the Azure Search index and
     * returns the results as a list of maps, where each map represents a matching
     * document.
     *
     * @param query      The search query string
     * @param maxResults The maximum number of results to return
     * @return A list of maps containing the search results, where each map
     *         represents a document
     */
    public List<Map<String, Object>> search(String query, int maxResults) {
        return search(query, maxResults, 0);
    }

    /**
     * Performs a paginated search against the index with the given query string.
     * <p>
     * This method executes a search operation against the Azure Search index with
     * pagination support and returns the results as a list of maps, where each map
     * represents a matching document.
     *
     * @param query      The search query string
     * @param pageSize   The number of results to return per page
     * @param pageOffset The skip/offset for pagination (0 for first page)
     * @return A list of maps containing the search results, where each map
     *         represents a document
     */
    public List<Map<String, Object>> search(String query, int pageSize, int pageOffset) {
        logger.info("Searching for: '{}' with page size: {}, offset: {}", query, pageSize, pageOffset);

        // Ensure valid page parameters
        int size = pageSize > 0 ? pageSize : 50; // Default to 50 if not specified
        int offset = Math.max(0, pageOffset); // Ensure non-negative offset

        SearchOptions options = new SearchOptions()
                .setTop(size)
                .setSkip(offset)
                .setIncludeTotalCount(true);

        List<Map<String, Object>> results = new ArrayList<>();
        try {
            searchClient.search(query, options, Context.NONE).forEach(result -> {
                results.add(result.getDocument(Map.class));
            });
            logger.info("Search returned {} documents", results.size());
        } catch (Exception e) {
            logger.error("Error performing search operation", e);
        }

        return results;
    }

    /**
     * Gets the total count of documents that match a query.
     * <p>
     * This method is useful for calculating pagination information before fetching
     * the actual results.
     *
     * @param query The search query string
     * @return The total count of matching documents, or -1 if an error occurred
     */
    public long getSearchResultCount(String query) {
        logger.info("Getting count for search query: '{}'", query);

        try {
            SearchOptions options = new SearchOptions()
                    .setTop(1) // Minimum results needed
                    .setIncludeTotalCount(true);

            return searchClient.search(query, options, Context.NONE).getTotalCount();
        } catch (Exception e) {
            logger.error("Error getting search result count", e);
            return -1;
        }
    }

    /**
     * Returns a map with pagination information for a search query.
     * <p>
     * This method provides metadata that can be used to build pagination UI
     * and handle page navigation.
     *
     * @param query       The search query string
     * @param pageSize    The number of results per page
     * @param currentPage The current page number (0-based)
     * @return A map containing pagination metadata
     */
    public Map<String, Object> getSearchPaginationInfo(String query, int pageSize, int currentPage) {
        logger.info("Getting pagination info for: '{}', pageSize: {}, currentPage: {}",
                query, pageSize, currentPage);

        Map<String, Object> paginationInfo = new HashMap<>();

        try {
            long totalCount = getSearchResultCount(query);
            if (totalCount < 0) {
                throw new RuntimeException("Could not retrieve search result count");
            }

            int size = pageSize > 0 ? pageSize : 50;
            int totalPages = (int) Math.ceil((double) totalCount / size);
            int current = Math.max(0, Math.min(currentPage, totalPages - 1));

            paginationInfo.put("totalCount", totalCount);
            paginationInfo.put("pageSize", size);
            paginationInfo.put("totalPages", totalPages);
            paginationInfo.put("currentPage", current);
            paginationInfo.put("hasNextPage", current < totalPages - 1);
            paginationInfo.put("hasPreviousPage", current > 0);

            return paginationInfo;
        } catch (Exception e) {
            logger.error("Error calculating pagination info", e);
            paginationInfo.put("error", e.getMessage());
            return paginationInfo;
        }
    }

    /**
     * Runs the indexer manually to update the search index.
     * <p>
     * This method triggers an immediate run of the indexer to process new or
     * changed content
     * in the data source and update the search index accordingly.
     * 
     * @return true if the indexer was successfully triggered, false otherwise
     */
    public boolean runIndexer() {
        try {
            // Run the indexer
            logger.info("Triggering indexer run for: {}", indexer.getName());
            indexerClient.runIndexer(indexer.getName());
            logger.info("Search indexer run triggered successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error running indexer: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cleans up all search resources created by this client instance.
     * <p>
     * This method deletes the data source, index, skillset, and indexer created by
     * this client.
     * It should be called when the search resources are no longer needed to avoid
     * consuming unnecessary resources in the Azure Search service.
     */
    public void cleanUp() {
        String datasourceName = dataSource.getName();
        String indexName = index.getName();
        String skillsetName = skillset.getName();
        String indexerName = indexer.getName();

        logger.info("Cleaning up search resources");

        try {
            indexerClient.deleteDataSourceConnection(datasourceName);
            logger.info("Data Source {} deleted successfully", datasourceName);

            indexClient.deleteIndex(indexName);
            logger.info("Index {} deleted successfully", indexName);

            indexerClient.deleteSkillset(skillsetName);
            logger.info("Skillset {} deleted successfully", skillsetName);

            indexerClient.deleteIndexer(indexerName);
            logger.info("Indexer {} deleted successfully", indexerName);

        } catch (Exception ex) {
            logger.error("Error deleting search resources", ex);
        }
    }
}