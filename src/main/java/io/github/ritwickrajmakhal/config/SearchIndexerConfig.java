package io.github.ritwickrajmakhal.config;

import com.azure.search.documents.indexes.SearchIndexerClient;
import com.azure.search.documents.indexes.models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchIndexerConfig {

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

    private static List<FieldMapping> createOutputFieldMappings() {
        List<FieldMapping> outputMappings = new ArrayList<>();

        // Add all the output field mappings
        outputMappings.add(new FieldMapping("/document/people").setTargetFieldName("people"));
        outputMappings.add(new FieldMapping("/document/organizations").setTargetFieldName("organizations"));
        outputMappings.add(new FieldMapping("/document/locations").setTargetFieldName("locations"));
        outputMappings.add(new FieldMapping("/document/language").setTargetFieldName("language"));
        outputMappings.add(new FieldMapping("/document/merged_content/keyphrases").setTargetFieldName("keyphrases"));
        outputMappings.add(new FieldMapping("/document/merged_content/translated_text").setTargetFieldName("translated_text"));
        outputMappings.add(new FieldMapping("/document/merged_content/pii_entities").setTargetFieldName("pii_entities"));
        outputMappings.add(new FieldMapping("/document/merged_content/masked_text").setTargetFieldName("masked_text"));
        outputMappings.add(new FieldMapping("/document/merged_content").setTargetFieldName("merged_content"));
        outputMappings.add(new FieldMapping("/document/normalized_images/*/text").setTargetFieldName("text"));
        outputMappings.add(new FieldMapping("/document/normalized_images/*/layoutText").setTargetFieldName("layoutText"));
        outputMappings.add(new FieldMapping("/document/normalized_images/*/imageTags/*/name").setTargetFieldName("imageTags"));
        outputMappings.add(new FieldMapping("/document/normalized_images/*/imageCaption").setTargetFieldName("imageCaption"));

        return outputMappings;
    }
}