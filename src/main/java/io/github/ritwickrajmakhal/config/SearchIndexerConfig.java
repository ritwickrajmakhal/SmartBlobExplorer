package io.github.ritwickrajmakhal.config;

import com.azure.search.documents.indexes.SearchIndexerClient;
import com.azure.search.documents.indexes.models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Azure AI Search indexers.
 * <p>
 * This class provides factory methods to create and configure search indexers
 * that connect data sources to search indexes while applying cognitive skills.
 * <p>
 * An indexer in Azure AI Search is responsible for:
 * <ul>
 * <li>Extracting data from a data source</li>
 * <li>Applying a skillset to enrich the data</li>
 * <li>Mapping source fields to destination fields in the index</li>
 * <li>Scheduling and executing the indexing process</li>
 * </ul>
 */
public class SearchIndexerConfig {

    /**
     * Creates an indexer that connects a data source to an index using a skillset.
     * <p>
     * This method configures the indexer with:
     * <ul>
     * <li>Indexer parameters for content extraction and image processing</li>
     * <li>Field mappings for handling source fields</li>
     * <li>Output field mappings that connect skillset outputs to index fields</li>
     * </ul>
     *
     * @param indexerClient  The client for managing indexers
     * @param indexerName    The name for the new indexer
     * @param dataSourceName The name of the data source to use
     * @param indexName      The name of the index to populate
     * @param skillsetName   The name of the skillset to apply
     * @return The created or updated search indexer
     * @throws RuntimeException if creation or update of the indexer fails
     */
    public static SearchIndexer createIndexer(
            SearchIndexerClient indexerClient,
            String indexerName,
            String dataSourceName,
            String indexName,
            String skillsetName) {

        // Create the indexer configuration
        SearchIndexer indexer = new SearchIndexer(indexerName, dataSourceName, indexName)
                .setSkillsetName(skillsetName)
                .setParameters(createIndexerParameters())
                .setOutputFieldMappings(createOutputFieldMappings())
                .setFieldMappings(createFieldMappings());

        // Create or update the indexer
        try {
            indexer = indexerClient.createOrUpdateIndexer(indexer);
            return indexer;
        } catch (Exception e) {
            System.err.println("Error creating or updating indexer: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates indexer parameters for content extraction and processing.
     * <p>
     * These parameters configure how the indexer processes data:
     * <ul>
     * <li>dataToExtract: Extracts both content and metadata from documents</li>
     * <li>parsingMode: Uses default parsing for documents</li>
     * <li>imageAction: Generates normalized images for OCR and image analysis</li>
     * </ul>
     * <p>
     * Also configures error handling to stop on first error.
     *
     * @return The configured indexing parameters
     */
    private static IndexingParameters createIndexerParameters() {
        // Create configuration map
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("dataToExtract", "contentAndMetadata");
        configuration.put("parsingMode", "default");
        configuration.put("imageAction", "generateNormalizedImages");

        // Create indexing parameters
        IndexingParameters parameters = new IndexingParameters()
                .setMaxFailedItems(0)
                .setMaxFailedItemsPerBatch(0)
                .setConfiguration(configuration);

        return parameters;
    }

    /**
     * Creates field mappings for the indexer.
     * <p>
     * Field mappings define how source fields are mapped to target fields in the
     * index.
     * This method creates a field mapping that Base64 encodes the
     * metadata_storage_path
     * field to ensure it's properly formatted as the document key.
     *
     * @return List of field mappings for the indexer
     */
    private static List<FieldMapping> createFieldMappings() {
        List<FieldMapping> fieldMappings = new ArrayList<>();

        // Create the base64Encode mapping function parameters
        Map<String, Object> functionParameters = new HashMap<>();
        functionParameters.put("useHttpServerUtilityUrlTokenEncode", false);

        // Create the mapping function
        FieldMappingFunction mappingFunction = new FieldMappingFunction("base64Encode")
                .setParameters(functionParameters);

        // Create the field mapping for metadata_storage_path
        FieldMapping fieldMapping = new FieldMapping("metadata_storage_path")
                .setTargetFieldName("metadata_storage_path")
                .setMappingFunction(mappingFunction);

        fieldMappings.add(fieldMapping);
        return fieldMappings;
    }

    /**
     * Creates output field mappings for the indexer.
     * <p>
     * Output field mappings define how the output from cognitive skills
     * is mapped to fields in the search index. These mappings connect the
     * structured data produced by the skillset to their corresponding fields.
     * <p>
     * Mappings include:
     * <ul>
     * <li>Entity extraction results (people, organizations, locations)</li>
     * <li>Language detection and translation</li>
     * <li>Key phrases from content</li>
     * <li>PII entity detection and masking</li>
     * <li>OCR and image analysis results</li>
     * </ul>
     *
     * @return List of output field mappings for the indexer
     */
    private static List<FieldMapping> createOutputFieldMappings() {
        List<FieldMapping> outputMappings = new ArrayList<>();

        // Add all the output field mappings
        outputMappings.add(new FieldMapping("/document/people").setTargetFieldName("people"));
        outputMappings.add(new FieldMapping("/document/organizations").setTargetFieldName("organizations"));
        outputMappings.add(new FieldMapping("/document/locations").setTargetFieldName("locations"));
        outputMappings.add(new FieldMapping("/document/language").setTargetFieldName("language"));
        outputMappings.add(new FieldMapping("/document/merged_content/keyphrases").setTargetFieldName("keyphrases"));
        outputMappings.add(
                new FieldMapping("/document/merged_content/translated_text").setTargetFieldName("translated_text"));
        outputMappings
                .add(new FieldMapping("/document/merged_content/pii_entities").setTargetFieldName("pii_entities"));
        outputMappings.add(new FieldMapping("/document/merged_content/masked_text").setTargetFieldName("masked_text"));
        outputMappings.add(new FieldMapping("/document/merged_content").setTargetFieldName("merged_content"));
        outputMappings.add(new FieldMapping("/document/normalized_images/*/text").setTargetFieldName("text"));
        outputMappings
                .add(new FieldMapping("/document/normalized_images/*/layoutText").setTargetFieldName("layoutText"));
        outputMappings.add(
                new FieldMapping("/document/normalized_images/*/imageTags/*/name").setTargetFieldName("imageTags"));
        outputMappings
                .add(new FieldMapping("/document/normalized_images/*/imageCaption").setTargetFieldName("imageCaption"));

        return outputMappings;
    }
}