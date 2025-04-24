package io.github.ritwickrajmakhal;

import java.util.ArrayList;
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
import com.azure.search.documents.indexes.models.DataChangeDetectionPolicy;
import com.azure.search.documents.indexes.models.SearchField;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.indexes.models.SearchIndexer;
import com.azure.search.documents.indexes.models.SearchIndexerDataContainer;
import com.azure.search.documents.indexes.models.SearchIndexerDataSourceConnection;
import com.azure.search.documents.indexes.models.SearchIndexerDataSourceType;
import com.azure.search.documents.indexes.models.SearchIndexerSkill;
import com.azure.search.documents.indexes.models.SearchIndexerSkillset;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;

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
        logger.debug("Initializing AzureSearchClient with container: {} and folder: {}", containerName, folder);
        this.blobStorageConnectionString = blobStorageConnectionString;
        this.containerName = containerName;
        this.folder = folder;

        // Create search indexer client
        logger.debug("Creating search indexer client");
        indexerClient = new SearchIndexerClientBuilder()
                .endpoint(SEARCH_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .buildClient();
        // Create search index client
        logger.debug("Creating search index client");
        indexClient = new SearchIndexClientBuilder()
                .endpoint(SEARCH_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .buildClient();

        // Create a data source for blob storage
        logger.debug("Creating blob data source");
        dataSource = createBlobDataSource(indexerClient);

        // Create a skillsset
        logger.debug("Creating skillset");
        skillset = createSkillset(indexerClient);

        // Create an index
        logger.debug("Creating index");
        index = createIndex(indexClient);

        // Create an indexer that uses the skillset and data source and loads the index
        logger.debug("Creating search indexer");
        indexer = createSearchIndexer(indexerClient, dataSource, index, skillset);

        logger.debug("Creating search client for index: {}", index.getName());
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
        logger.debug("Creating skillset with default skills");
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
        logger.debug("Creating search index with default fields");
        List<SearchField> fields = SearchIndexConfig.getDefaultFields();
        // Index definition
        SearchIndex index = new SearchIndex("index-" + uuid, fields);

        // Set Suggester
        logger.debug("Setting default suggester for index");
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
        logger.debug("Creating search indexer: {}", indexerName);
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
     * @param client                    The search indexer client to use
     * @param type                      The type of data source (e.g., blob, table,
     *                                  cosmos)
     * @param connectionString          The connection string for the data source
     * @param container                 The container information for the data
     *                                  source
     * @param dataChangeDetectionPolicy Optional policy for detecting changes in the
     *                                  data source
     * @return The created or updated data source connection
     */
    private static SearchIndexerDataSourceConnection createDataSource(
            final SearchIndexerClient client,
            final SearchIndexerDataSourceType type,
            final String connectionString,
            final SearchIndexerDataContainer container,
            final DataChangeDetectionPolicy dataChangeDetectionPolicy) {

        String datasourceName = "datasource-" + uuid;
        logger.debug("Creating data source: {}", datasourceName);
        SearchIndexerDataSourceConnection dataSource = new SearchIndexerDataSourceConnection(
                datasourceName,
                type,
                connectionString,
                container)
                .setDataChangeDetectionPolicy(dataChangeDetectionPolicy);

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
            logger.debug("Creating data container for whole blob container: {}", containerName);
            dataContainer = new SearchIndexerDataContainer(containerName);
        } else {
            logger.debug("Creating data container for folder: {} in container: {}", folder, containerName);
            dataContainer = new SearchIndexerDataContainer(containerName).setQuery(folder);
        }
        return createDataSource(
                client,
                SearchIndexerDataSourceType.AZURE_BLOB,
                this.blobStorageConnectionString,
                dataContainer,
                null);
    }

    /**
     * Performs a search against the index with the given query string.
     * <p>
     * This method executes a search operation against the Azure Search index and
     * returns
     * the results as a list of maps, where each map represents a matching document.
     *
     * @param query      The search query string
     * @param maxResults The maximum number of results to return
     * @return A list of maps containing the search results, where each map
     *         represents a document
     */
    public List<Map<String, Object>> search(String query, int maxResults) {
        logger.info("Searching for: '{}' with max results: {}", query, maxResults);
        SearchOptions options = new SearchOptions()
                .setTop(maxResults);

        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchResult result : searchClient.search(query, options, Context.NONE)) {
            results.add(result.getDocument(Map.class));
        }
        logger.debug("Search returned {} results", results.size());
        return results;
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
            logger.debug("Deleting data source: {}", datasourceName);
            indexerClient.deleteDataSourceConnection(datasourceName);
            logger.info("Data Source {} deleted successfully", datasourceName);

            logger.debug("Deleting index: {}", indexName);
            indexClient.deleteIndex(indexName);
            logger.info("Index {} deleted successfully", indexName);

            logger.debug("Deleting skillset: {}", skillsetName);
            indexerClient.deleteSkillset(skillsetName);
            logger.info("Skillset {} deleted successfully", skillsetName);

            logger.debug("Deleting indexer: {}", indexerName);
            indexerClient.deleteIndexer(indexerName);
            logger.info("Indexer {} deleted successfully", indexerName);

        } catch (Exception ex) {
            logger.error("Error deleting search resources", ex);
        }
    }
}