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

public class AzureSearchClient {
    private final static Dotenv dotenv = Dotenv.load();
    private final static String uuid = UUID.randomUUID().toString();
    private static final String SEARCH_ENDPOINT = dotenv.get("AZURE_AI_SEARCH_ENDPOINT");
    private static final String SEARCH_API_KEY = dotenv.get("AZURE_AI_SEARCH_API_KEY");
    private final String blobStorageConnectionString;
    private final String containerName;
    private final String folder;

    // resources
    private final SearchIndexerDataSourceConnection dataSource;
    private final SearchIndex index;
    private final SearchIndexerSkillset skillset;
    private final SearchIndexer indexer;

    // clients
    private final SearchIndexerClient indexerClient;
    private final SearchIndexClient indexClient;
    private final SearchClient searchClient;

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
    }

    private static SearchIndexerSkillset createSkillset(final SearchIndexerClient client) {
        List<SearchIndexerSkill> skills = SkillsetConfig.getDefaultSkills();
        SearchIndexerSkillset skillset = new SearchIndexerSkillset("skillset-" + uuid, skills)
                .setDescription("Skillset for testing default configuration")
                .setCognitiveServicesAccount(SkillsetConfig.getCognitiveServicesConfig());

        return client.createOrUpdateSkillset(skillset);
    }

    private static SearchIndex createIndex(final SearchIndexClient client) {
        List<SearchField> fields = SearchIndexConfig.getDefaultFields();
        // Index definition
        SearchIndex index = new SearchIndex("index-" + uuid, fields);

        // Set Suggester
        index.setSuggesters(SearchIndexConfig.getDefaultSuggester());

        return client.createOrUpdateIndex(index);
    }

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

    private static SearchIndexerDataSourceConnection createDataSource(
            final SearchIndexerClient client,
            final SearchIndexerDataSourceType type,
            final String connectionString,
            final SearchIndexerDataContainer container,
            final DataChangeDetectionPolicy dataChangeDetectionPolicy) {

        SearchIndexerDataSourceConnection dataSource = new SearchIndexerDataSourceConnection(
                "datasource-" + uuid,
                type,
                connectionString,
                container)
                .setDataChangeDetectionPolicy(dataChangeDetectionPolicy);

        try {
            return client.createOrUpdateDataSourceConnection(dataSource);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return dataSource;
    }

    private SearchIndexerDataSourceConnection createBlobDataSource(final SearchIndexerClient client) {
        SearchIndexerDataContainer dataContainer;
        if (folder == "") {
            dataContainer = new SearchIndexerDataContainer(containerName);
        } else {
            dataContainer = new SearchIndexerDataContainer(containerName).setQuery(folder);
        }
        return createDataSource(
                client,
                SearchIndexerDataSourceType.AZURE_BLOB,
                this.blobStorageConnectionString,
                dataContainer,
                null);
    }

    public List<Map<String, Object>> search(String query, int maxResults) {
        SearchOptions options = new SearchOptions()
                .setTop(maxResults);

        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchResult result : searchClient.search(query, options, Context.NONE)) {
            results.add(result.getDocument(Map.class));
        }
        return results;
    }

    /**
     * Runs the indexer manually to update the search index
     * 
     * @return true if the indexer was successfully triggered, false otherwise
     */
    public boolean runIndexer() {
        try {
            // Run the indexer
            indexerClient.runIndexer(indexer.getName());
            System.out.println("🔄 Search indexer run triggered successfully.");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error running indexer: " + e.getMessage());
            return false;
        }
    }

    public void cleanUp() {
        String datasourceName = dataSource.getName();
        String indexName = index.getName();
        String skillsetName = skillset.getName();
        String indexerName = indexer.getName();
        try {
            indexerClient.deleteDataSourceConnection(datasourceName);
            System.out.println("Data Source " + datasourceName + " deleted successfully.");

            indexClient.deleteIndex(indexName);
            System.out.println("Index " + indexName + " deleted successfully");

            indexerClient.deleteSkillset(skillsetName);
            System.out.println("Skillset " + skillsetName + " deleted successfully");

            indexerClient.deleteIndexer(indexerName);
            System.out.println("Indexer " + indexerName + " deleted successfully.");

        } catch (Exception ex) {
            System.err.println("Error deleting data source: " + ex.toString());
        }
    }
}