package io.github.ritwickrajmakhal.config;

import com.azure.search.documents.indexes.models.*;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for Azure AI Search cognitive skills.
 * <p>
 * This class provides factory methods to create a complete skillset with
 * various
 * cognitive skills for enriching content during the indexing process. Each
 * skill
 * extracts different types of information or transforms content in specific
 * ways.
 * <p>
 * The skillset includes:
 * <ul>
 * <li>Entity recognition for extracting people, organizations, and
 * locations</li>
 * <li>Key phrase extraction for identifying important concepts</li>
 * <li>Language detection to identify the document language</li>
 * <li>Text translation to convert content to English</li>
 * <li>PII (Personally Identifiable Information) detection and masking</li>
 * <li>Content merging to combine document text with OCR results</li>
 * <li>OCR (Optical Character Recognition) for text extraction from images</li>
 * <li>Image analysis for tagging and captioning images</li>
 * </ul>
 * <p>
 * These skills work together in a pipeline to create enriched content that is
 * stored in the search index.
 */
public class SkillsetConfig {
        /** Environment variable loader for accessing configuration values */
        private final static Dotenv dotenv = Dotenv.load();

        /**
         * Creates and returns a complete set of cognitive skills for document
         * processing.
         * <p>
         * This method assembles all the skills needed for comprehensive document
         * analysis,
         * including text analysis, entity extraction, and image processing.
         * <p>
         * The skills are designed to work together in a pipeline, with outputs from
         * some skills feeding into inputs of others.
         *
         * @return A list of SearchIndexerSkill objects configured for document
         *         processing
         */
        public static List<SearchIndexerSkill> getDefaultSkills() {
                List<SearchIndexerSkill> skills = new ArrayList<>();

                // Entity Recognition Skill
                skills.add(createEntityRecognitionSkill());

                // Key Phrase Extraction Skill
                skills.add(createKeyPhraseExtractionSkill());

                // Language Detection Skill
                skills.add(createLanguageDetectionSkill());

                // Translation Skill
                skills.add(createTranslationSkill());

                // PII Detection Skill
                skills.add(createPIIDetectionSkill());

                // Merge Skill
                skills.add(createMergeSkill());

                // OCR Skill
                skills.add(createOcrSkill());

                // Image Analysis Skill
                skills.add(createImageAnalysisSkill());

                return skills;
        }

        /**
         * Creates a skill for recognizing named entities in text.
         * <p>
         * This skill identifies and categorizes entities such as people, organizations,
         * locations, quantities, and dates mentioned in the document.
         *
         * @return An EntityRecognitionSkill configured for entity extraction
         */
        private static SearchIndexerSkill createEntityRecognitionSkill() {
                List<InputFieldMappingEntry> inputs = Arrays.asList(
                                new InputFieldMappingEntry("text")
                                                .setSource("/document/merged_content"),
                                new InputFieldMappingEntry("languageCode")
                                                .setSource("/document/language"));

                List<OutputFieldMappingEntry> outputs = Arrays.asList(
                                new OutputFieldMappingEntry("persons")
                                                .setTargetName("people"),
                                new OutputFieldMappingEntry("organizations")
                                                .setTargetName("organizations"),
                                new OutputFieldMappingEntry("locations")
                                                .setTargetName("locations"));

                List<EntityCategory> categories = Arrays.asList(
                                EntityCategory.PERSON,
                                EntityCategory.DATETIME,
                                EntityCategory.URL,
                                EntityCategory.EMAIL,
                                EntityCategory.LOCATION,
                                EntityCategory.PERSON,
                                EntityCategory.QUANTITY,
                                EntityCategory.ORGANIZATION);

                return new EntityRecognitionSkill(inputs, outputs, EntityRecognitionSkillVersion.V3)
                                .setName("#1")
                                .setDescription("Entity Recognition Skill")
                                .setContext("/document/merged_content")
                                .setDefaultLanguageCode(EntityRecognitionSkillLanguage.EN)
                                .setCategories(categories);
        }

        /**
         * Creates a skill for extracting key phrases from text.
         * <p>
         * Key phrases are important concepts or topics mentioned in the document
         * that can help with document summarization and semantic search.
         *
         * @return A KeyPhraseExtractionSkill configured for key phrase extraction
         */
        private static SearchIndexerSkill createKeyPhraseExtractionSkill() {
                List<InputFieldMappingEntry> inputs = Arrays.asList(
                                new InputFieldMappingEntry("text")
                                                .setSource("/document/merged_content"),
                                new InputFieldMappingEntry("languageCode")
                                                .setSource("/document/language"));

                List<OutputFieldMappingEntry> outputs = Collections.singletonList(
                                new OutputFieldMappingEntry("keyPhrases")
                                                .setTargetName("keyphrases"));

                return new KeyPhraseExtractionSkill(inputs, outputs)
                                .setName("#2")
                                .setContext("/document/merged_content")
                                .setDefaultLanguageCode(KeyPhraseExtractionSkillLanguage.EN);
        }

        /**
         * Creates a skill for detecting the language of document content.
         * <p>
         * Language detection identifies the primary language of the text,
         * which is used by other language-dependent skills in the pipeline.
         *
         * @return A LanguageDetectionSkill configured for language identification
         */
        private static SearchIndexerSkill createLanguageDetectionSkill() {
                List<InputFieldMappingEntry> inputs = Collections.singletonList(
                                new InputFieldMappingEntry("text")
                                                .setSource("/document/merged_content"));

                List<OutputFieldMappingEntry> outputs = Collections.singletonList(
                                new OutputFieldMappingEntry("languageCode")
                                                .setTargetName("language"));

                return new LanguageDetectionSkill(inputs, outputs)
                                .setName("#3")
                                .setContext("/document");
        }

        /**
         * Creates a skill for translating text to English.
         * <p>
         * This skill translates document content to English, making it
         * searchable for users regardless of the original language.
         *
         * @return A TextTranslationSkill configured for translation to English
         */
        private static SearchIndexerSkill createTranslationSkill() {
                List<InputFieldMappingEntry> inputs = Collections.singletonList(
                                new InputFieldMappingEntry("text")
                                                .setSource("/document/merged_content"));

                List<OutputFieldMappingEntry> outputs = Collections.singletonList(
                                new OutputFieldMappingEntry("translatedText")
                                                .setTargetName("translated_text"));

                return new TextTranslationSkill(inputs, outputs, TextTranslationSkillLanguage.EN)
                                .setName("#4")
                                .setContext("/document/merged_content");
        }

        /**
         * Creates a skill for detecting and masking personally identifiable information
         * (PII).
         * <p>
         * This skill identifies sensitive information like credit card numbers, phone
         * numbers,
         * addresses, etc., and can provide both the detected entities and a masked
         * version
         * of the text where these entities are replaced with asterisks.
         *
         * @return A PiiDetectionSkill configured for PII detection and masking
         */
        private static SearchIndexerSkill createPIIDetectionSkill() {
                List<InputFieldMappingEntry> inputs = Arrays.asList(
                                new InputFieldMappingEntry("text")
                                                .setSource("/document/merged_content"),
                                new InputFieldMappingEntry("languageCode")
                                                .setSource("/document/language"));

                List<OutputFieldMappingEntry> outputs = Arrays.asList(
                                new OutputFieldMappingEntry("piiEntities")
                                                .setTargetName("pii_entities"),
                                new OutputFieldMappingEntry("maskedText")
                                                .setTargetName("masked_text"));

                return new PiiDetectionSkill(inputs, outputs)
                                .setName("#5")
                                .setContext("/document/merged_content")
                                .setDefaultLanguageCode("en")
                                .setMinimumPrecision(0.5)
                                .setMaskingMode(PiiDetectionSkillMaskingMode.REPLACE)
                                .setDomain("none");
        }

        /**
         * Creates a skill for merging extracted text from images with document content.
         * <p>
         * This skill combines the original document text with text extracted from
         * images
         * via OCR, creating a unified text representation of the document's full
         * content.
         * The merged content becomes the input for most other text analysis skills.
         *
         * @return A MergeSkill configured to combine document content with OCR results
         */
        private static SearchIndexerSkill createMergeSkill() {
                List<InputFieldMappingEntry> inputs = Arrays.asList(
                                new InputFieldMappingEntry("text")
                                                .setSource("/document/content"),
                                new InputFieldMappingEntry("itemsToInsert")
                                                .setSource("/document/normalized_images/*/text"), // Output from OCR
                                                                                                  // Skill
                                new InputFieldMappingEntry("offsets")
                                                .setSource("/document/normalized_images/*/contentOffset"));

                List<OutputFieldMappingEntry> outputs = Collections.singletonList(
                                new OutputFieldMappingEntry("mergedText")
                                                .setTargetName("merged_content")); // Target field for merged content

                return new MergeSkill(inputs, outputs)
                                .setName("#6")
                                .setContext("/document")
                                .setInsertPreTag(" ") // Add a space before inserted items
                                .setInsertPostTag(" "); // Add a space after inserted items
        }

        /**
         * Creates a skill for optical character recognition (OCR) on images.
         * <p>
         * This skill extracts text from images found in documents, making image content
         * searchable. It can produce both plain text and text with layout information.
         *
         * @return An OcrSkill configured for text extraction from images
         */
        private static SearchIndexerSkill createOcrSkill() {
                // Define the inputs for the OcrSkill
                List<InputFieldMappingEntry> inputs = Collections.singletonList(
                                new InputFieldMappingEntry("image")
                                                .setSource("/document/normalized_images/*"));

                // Define the outputs for the OcrSkill
                List<OutputFieldMappingEntry> outputs = Arrays.asList(
                                new OutputFieldMappingEntry("text")
                                                .setTargetName("text"),
                                new OutputFieldMappingEntry("layoutText")
                                                .setTargetName("layoutText"));

                // Create and return the OcrSkill
                return new OcrSkill(inputs, outputs)
                                .setName("#7")
                                .setContext("/document/normalized_images/*")
                                .setDefaultLanguageCode(OcrSkillLanguage.EN)
                                .setShouldDetectOrientation(true);
        }

        /**
         * Creates a skill for analyzing image content.
         * <p>
         * This skill identifies objects, scenes, and concepts in images and generates
         * descriptive tags and captions to make images searchable by their visual
         * content.
         *
         * @return An ImageAnalysisSkill configured for image tagging and captioning
         */
        private static SearchIndexerSkill createImageAnalysisSkill() {
                // Define inputs: Ensure the source contains valid image data
                List<InputFieldMappingEntry> inputs = Collections.singletonList(
                                new InputFieldMappingEntry("image")
                                                .setSource("/document/normalized_images/*"));

                // Define outputs
                List<OutputFieldMappingEntry> outputs = Arrays.asList(
                                new OutputFieldMappingEntry("tags")
                                                .setTargetName("imageTags"),
                                new OutputFieldMappingEntry("description")
                                                .setTargetName("imageCaption"));

                // This is the key change - use strings directly instead of enums
                List<VisualFeature> visualFeatures = Arrays.asList(VisualFeature.TAGS, VisualFeature.DESCRIPTION);

                // Create and return the ImageAnalysisSkill
                return new ImageAnalysisSkill(inputs, outputs)
                                .setName("#8")
                                .setContext("/document/normalized_images/*") // Match the exact context path from JSON
                                .setDefaultLanguageCode(ImageAnalysisSkillLanguage.EN)
                                .setVisualFeatures(visualFeatures)
                                .setDetails(Collections.emptyList());
        }

        /**
         * Creates and returns the cognitive services account configuration.
         * <p>
         * This configuration connects the skillset to an Azure Cognitive Services
         * account
         * that provides the AI capabilities needed for the cognitive skills.
         * The account key is retrieved from environment variables.
         *
         * @return A CognitiveServicesAccountKey object with the account credentials
         */
        public static CognitiveServicesAccountKey getCognitiveServicesConfig() {
                return new CognitiveServicesAccountKey(dotenv.get("COGNITIVE_SERVICE_ACCOUNT_KEY"))
                                .setDescription(
                                                "This service is used to perform various cognitive tasks such as OCR, image analysis, and language detection.");
        }
}