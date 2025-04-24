package io.github.ritwickrajmakhal;

import com.azure.ai.openai.models.FunctionDefinition;
import com.azure.core.util.BinaryData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registry for Azure OpenAI function definitions.
 * <p>
 * This class provides function definitions that describe the API contract
 * for the SmartBlobExplorer functions. These definitions are used to register
 * the functions with the Azure OpenAI service, enabling the AI model to
 * understand
 * and call the appropriate functions based on user queries.
 * <p>
 * Each function definition includes:
 * <ul>
 * <li>A name that identifies the function</li>
 * <li>A description that explains what the function does</li>
 * <li>Parameters that specify the input schema using JSON Schema format</li>
 * </ul>
 */
public class FunctionRegistry {

    /**
     * Creates and returns function definitions for blob management operations.
     * <p>
     * This method defines the following functions for blob operations:
     * <ul>
     * <li>upload_file - Uploads a local file or file from URL to blob storage</li>
     * <li>download_blob - Downloads a blob to a local file</li>
     * <li>delete_blob - Deletes a blob from storage</li>
     * <li>rename_blob - Renames or moves a blob within storage</li>
     * <li>search_blobs - Searches blobs using Azure AI Search</li>
     * </ul>
     * <p>
     * Each function definition includes a JSON Schema that describes the
     * parameters expected by the function, including which ones are required
     * and their data types.
     *
     * @return A list of FunctionDefinition objects for blob management operations
     */
    public static List<FunctionDefinition> getBlobFunctionDefinitions() {
        List<FunctionDefinition> functions = new ArrayList<>();

        // Upload file function
        functions.add(new FunctionDefinition("upload_file")
                .setDescription("Uploads a file to the container")
                .setParameters(BinaryData.fromObject(
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "filePath", Map.of(
                                                "type", "string",
                                                "description", "Path to the file to upload (local or URL)")),
                                "required", List.of("filePath")))));

        // Download blob function
        functions.add(new FunctionDefinition("download_blob")
                .setDescription("Downloads a blob from the container")
                .setParameters(BinaryData.fromObject(
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "blobName", Map.of(
                                                "type", "string",
                                                "description", "Name of the blob to download"),
                                        "destinationPath", Map.of(
                                                "type", "string",
                                                "description", "Path to save the downloaded blob")),
                                "required", List.of("blobName", "destinationPath")))));

        // Delete blob function
        functions.add(new FunctionDefinition("delete_blob")
                .setDescription("Deletes a blob from the container")
                .setParameters(BinaryData.fromObject(
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "blobName", Map.of(
                                                "type", "string",
                                                "description", "Name of the blob to delete")),
                                "required", List.of("blobName")))));

        // Rename blob function
        functions.add(new FunctionDefinition("rename_blob")
                .setDescription("Renames or moves a blob within the container")
                .setParameters(BinaryData.fromObject(
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "sourceBlobName", Map.of(
                                                "type", "string",
                                                "description", "Name of the source blob"),
                                        "destinationBlobName", Map.of(
                                                "type", "string",
                                                "description", "New name or path for the blob")),
                                "required", List.of("sourceBlobName", "destinationBlobName")))));

        // Search blobs function (Azure AI Search service)
        functions.add(new FunctionDefinition("search_blobs")
                .setDescription("Search for blobs in Azure Storage by query")
                .setParameters(BinaryData.fromObject(
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query", Map.of(
                                                "type", "string",
                                                "description", "Query to search for blobs"),
                                        "maxResults", Map.of(
                                                "type", "integer",
                                                "description", "Maximum number of results to return (default: 5)"),
                                        "includeContent", Map.of(
                                                "type", "boolean",
                                                "description",
                                                "Whether to include content summaries (default: false)")),
                                "required", List.of("query", "maxResults")))));

        return functions;
    }
}