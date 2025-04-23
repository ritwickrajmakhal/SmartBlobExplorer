package io.github.ritwickrajmakhal.config;

import com.azure.search.documents.indexes.models.*;

import java.util.Arrays;
import java.util.List;

public class SearchIndexConfig {

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
                
                new SearchField("people", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                        .setSearchable(Boolean.TRUE)
                        .setFilterable(Boolean.FALSE)
                        .setStored(Boolean.TRUE)
                        .setSortable(Boolean.FALSE)
                        .setFacetable(Boolean.FALSE)
                        .setKey(Boolean.FALSE)
                        .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),
                
                new SearchField("organizations", SearchFieldDataType.collection(SearchFieldDataType.STRING))
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
                
                new SearchField("keyphrases", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                        .setSearchable(Boolean.TRUE)
                        .setFilterable(Boolean.FALSE)
                        .setStored(Boolean.TRUE)
                        .setSortable(Boolean.FALSE)
                        .setFacetable(Boolean.FALSE)
                        .setKey(Boolean.FALSE)
                        .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),
                
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
                
                createPiiEntitiesComplexField(),
                
                new SearchField("masked_text", SearchFieldDataType.STRING)
                        .setSearchable(Boolean.TRUE)
                        .setFilterable(Boolean.FALSE)
                        .setStored(Boolean.TRUE)
                        .setSortable(Boolean.FALSE)
                        .setFacetable(Boolean.FALSE)
                        .setKey(Boolean.FALSE)
                        .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),
                
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
                
                new SearchField("layoutText", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                        .setSearchable(Boolean.TRUE)
                        .setFilterable(Boolean.FALSE)
                        .setStored(Boolean.TRUE)
                        .setSortable(Boolean.FALSE)
                        .setFacetable(Boolean.FALSE)
                        .setKey(Boolean.FALSE)
                        .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),
                
                new SearchField("imageTags", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                        .setSearchable(Boolean.TRUE)
                        .setFilterable(Boolean.FALSE)
                        .setStored(Boolean.TRUE)
                        .setSortable(Boolean.FALSE)
                        .setFacetable(Boolean.FALSE)
                        .setKey(Boolean.FALSE)
                        .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE),
                
                new SearchField("imageCaption", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                        .setSearchable(Boolean.TRUE)
                        .setFilterable(Boolean.FALSE)
                        .setStored(Boolean.TRUE)
                        .setSortable(Boolean.FALSE)
                        .setFacetable(Boolean.FALSE)
                        .setKey(Boolean.FALSE)
                        .setAnalyzerName(LexicalAnalyzerName.STANDARD_LUCENE)
        );
    }

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
                        .setKey(Boolean.FALSE)
        );

        return new SearchField("pii_entities", SearchFieldDataType.collection(SearchFieldDataType.COMPLEX))
                .setFields(piiSubfields);
    }

    public static SearchSuggester getDefaultSuggester() {
        return new SearchSuggester("sg", Arrays.asList("people", "organizations", "locations", "keyphrases"));
    }
}