package io.github.ritwickrajmakhal.config;

import com.azure.search.documents.indexes.models.*;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SkillsetConfig {
        private final static Dotenv dotenv = Dotenv.load();

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

        public static CognitiveServicesAccountKey getCognitiveServicesConfig() {
                return new CognitiveServicesAccountKey(dotenv.get("COGNITIVE_SERVICE_ACCOUNT_KEY"))
                                .setDescription(
                                                "/subscriptions/fd00280e-c83c-4071-88a3-4d4e0aa6aa2d/resourceGroups/my-ai-resources/providers/Microsoft.CognitiveServices/accounts/smart-blob-search-ai");
        }
}