package io.github.ritwickrajmakhal.config;

import com.azure.search.documents.indexes.models.*;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for Azure AI Search index fields and suggesters.
 * <p>
 * This class provides factory methods to create a standardized set of fields
 * for an Azure AI Search index, specifically designed for content extracted
 * from documents using cognitive skills. The fields include:
 * <ul>
 * <li>Document content and metadata</li>
 * <li>Extracted entities (people, organizations, locations)</li>
 * <li>Key phrases and language information</li>
 * <li>PII (Personally Identifiable Information) entities</li>
 * <li>Text extracted from images</li>
 * <li>Layout information and image captions</li>
 * </ul>
 */
public class SearchIndexConfig {

        /**
         * Creates and returns the default set of fields for an Azure Search index.
         * <p>
         * These fields are designed to store both the original document content and
         * the enriched metadata extracted by cognitive skills in the skillset.
         *
         * @return A list of SearchField objects configured for document content and
         *         enrichments
         */
        public static List<SearchField> getDefaultFields() {
                return Arrays.asList(
                                new SearchField("content", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                // Document metadata fields
                                new SearchField("metadata_storage_content_type", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE),

                                new SearchField("metadata_storage_size", SearchFieldDataType.INT64)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE),

                                new SearchField("metadata_storage_last_modified", SearchFieldDataType.DATE_TIME_OFFSET)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE),

                                new SearchField("metadata_storage_name", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE),

                                // Primary key field - the blob path is used as the unique identifier
                                new SearchField("metadata_storage_path", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.TRUE),

                                new SearchField("metadata_storage_file_extension", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE),

                                new SearchField("metadata_content_type", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE),

                                // Entity extraction fields
                                new SearchField("people", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("organizations",
                                                SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("locations", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                // Key phrase field
                                new SearchField("keyphrases",
                                                SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                // Language and translation fields
                                new SearchField("language", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("translated_text", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.EN_LUCENE),

                                // PII entities complex field
                                createPiiEntitiesComplexField(),

                                // PII masked text field
                                new SearchField("masked_text", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                // Content fields
                                new SearchField("merged_content", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("text", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("layoutText",
                                                SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                // Image analysis fields
                                new SearchField("imageTags", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("imageCaption",
                                                SearchFieldDataType.collection(SearchFieldDataType.STRING))
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE));
        }

        /**
         * Creates a complex field for storing PII (Personally Identifiable Information)
         * entities.
         * <p>
         * This field stores structured information about detected PII entities
         * including:
         * <ul>
         * <li>The text of the PII entity</li>
         * <li>The type and subtype of the entity</li>
         * <li>The position (offset and length) in the original text</li>
         * <li>The confidence score of the detection</li>
         * </ul>
         *
         * @return A complex SearchField for PII entity information
         */
        private static SearchField createPiiEntitiesComplexField() {
                List<SearchField> piiSubfields = Arrays.asList(
                                new SearchField("text", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("type", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("subtype", SearchFieldDataType.STRING)
                                                .setSearchable(Boolean.TRUE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE)
                                                .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),

                                new SearchField("offset", SearchFieldDataType.INT32)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE),

                                new SearchField("length", SearchFieldDataType.INT32)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE),

                                new SearchField("score", SearchFieldDataType.DOUBLE)
                                                .setSearchable(Boolean.FALSE)
                                                .setFilterable(Boolean.FALSE)
                                                .setStored(Boolean.TRUE)
                                                .setSortable(Boolean.FALSE)
                                                .setFacetable(Boolean.FALSE)
                                                .setKey(Boolean.FALSE));

                return new SearchField("pii_entities", SearchFieldDataType.collection(SearchFieldDataType.COMPLEX))
                                .setFields(piiSubfields);
        }

        /**
         * Creates and returns the default suggester for autocomplete functionality.
         * <p>
         * This suggester enables typeahead search suggestions based on entities and key
         * phrases
         * extracted from the document content.
         *
         * @return A SearchSuggester configured for entity and keyphrase fields
         */
        public static SearchSuggester getDefaultSuggester() {
                return new SearchSuggester("sg", Arrays.asList("people", "organizations", "locations", "keyphrases"));
        }
}