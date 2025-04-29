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
         * <li>batch_upload - Uploads multiple files at once</li>
         * <li>batch_download - Downloads multiple blobs at once</li>
         * <li>batch_delete - Deletes multiple blobs at once</li>
         * <li>list_blobs - Lists blobs with filtering options</li>
         * <li>copy_blob - Copies a blob while preserving the original</li>
         * <li>create_snapshot - Creates point-in-time snapshots of blobs</li>
         * <li>generate_sas_url - Generates temporary access URLs for blobs</li>
         * <li>list_local_files - Lists files in a local directory with optional
         * filtering</li>
         * <li>upload_directory - Uploads all files from a local directory to blob
         * storage</li>
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
                                                                                                "description",
                                                                                                "Path to the file to upload (local or URL)")),
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
                                                                                                "description",
                                                                                                "Name of the blob to download"),
                                                                                "destinationPath", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Path to save the downloaded blob")),
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
                                                                                                "description",
                                                                                                "Name of the blob to delete")),
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
                                                                                                "description",
                                                                                                "Name of the source blob"),
                                                                                "destinationBlobName", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "New name or path for the blob")),
                                                                "required",
                                                                List.of("sourceBlobName", "destinationBlobName")))));

                // Search blobs function (Azure AI Search service)
                functions.add(new FunctionDefinition("search_blobs")
                                .setDescription("Search for blobs in Azure Storage by query")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "query", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Query to search for blobs"),
                                                                                "maxResults", Map.of(
                                                                                                "type", "integer",
                                                                                                "description",
                                                                                                "Maximum number of results to return (default: 5)"),
                                                                                "includeContent", Map.of(
                                                                                                "type", "boolean",
                                                                                                "description",
                                                                                                "Whether to include content summaries (default: false)")),
                                                                "required", List.of("query", "maxResults")))));

                // Batch upload function
                functions.add(new FunctionDefinition("batch_upload")
                                .setDescription("Uploads multiple files to the container at once")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "filePaths", Map.of(
                                                                                                "type", "array",
                                                                                                "description",
                                                                                                "List of file paths or URLs to upload",
                                                                                                "items",
                                                                                                Map.of("type", "string"))),
                                                                "required", List.of("filePaths")))));

                // Batch download function
                functions.add(new FunctionDefinition("batch_download")
                                .setDescription("Downloads multiple blobs from the container at once")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "blobNames", Map.of(
                                                                                                "type", "array",
                                                                                                "description",
                                                                                                "List of blob names to download",
                                                                                                "items",
                                                                                                Map.of("type", "string")),
                                                                                "destinationDir", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Directory path to save the downloaded blobs")),
                                                                "required", List.of("blobNames", "destinationDir")))));

                // Batch delete function
                functions.add(new FunctionDefinition("batch_delete")
                                .setDescription("Deletes multiple blobs from the container at once")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "blobNames", Map.of(
                                                                                                "type", "array",
                                                                                                "description",
                                                                                                "List of blob names to delete",
                                                                                                "items",
                                                                                                Map.of("type", "string"))),
                                                                "required", List.of("blobNames")))));

                // List blobs function
                functions.add(new FunctionDefinition("list_blobs")
                                .setDescription("Lists blobs in the container with filtering options")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "prefix", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Prefix filter for blob names (optional)"),
                                                                                "regex", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Regular expression filter for blob names (optional)"),
                                                                                "maxResults", Map.of(
                                                                                                "type", "integer",
                                                                                                "description",
                                                                                                "Maximum number of results to return (0 for no limit, optional)")),
                                                                "required", List.of()))));

                // Copy blob function
                functions.add(new FunctionDefinition("copy_blob")
                                .setDescription("Copies a blob while preserving the original (unlike rename which moves it)")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "sourceBlobName", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Name of the source blob to copy"),
                                                                                "destinationBlobName", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Name for the new copy of the blob")),
                                                                "required",
                                                                List.of("sourceBlobName", "destinationBlobName")))));

                // Create snapshot function
                functions.add(new FunctionDefinition("create_snapshot")
                                .setDescription("Creates a point-in-time snapshot of a blob for version control")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "blobName", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Name of the blob to create a snapshot of")),
                                                                "required", List.of("blobName")))));

                // Generate SAS URL function
                functions.add(new FunctionDefinition("generate_sas_url")
                                .setDescription("Generates a temporary shared access signature (SAS) URL for a blob")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "blobName", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Name of the blob to generate a SAS URL for"),
                                                                                "durationHours", Map.of(
                                                                                                "type", "integer",
                                                                                                "description",
                                                                                                "Number of hours the SAS URL should be valid (default: 24)"),
                                                                                "readPermission", Map.of(
                                                                                                "type", "boolean",
                                                                                                "description",
                                                                                                "Whether the SAS should allow read access (default: true)"),
                                                                                "writePermission", Map.of(
                                                                                                "type", "boolean",
                                                                                                "description",
                                                                                                "Whether the SAS should allow write access (default: false)"),
                                                                                "deletePermission", Map.of(
                                                                                                "type", "boolean",
                                                                                                "description",
                                                                                                "Whether the SAS should allow delete access (default: false)")),
                                                                "required", List.of("blobName")))));

                // List local files function
                functions.add(new FunctionDefinition("list_local_files")
                                .setDescription("Lists files in a local directory with optional filtering")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "directoryPath", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Path to the directory to list (e.g., 'Downloads', '~/Documents', etc.)"),
                                                                                "pattern", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Optional regex pattern to filter files by name (e.g., '.*\\.pdf' for PDF files)"),
                                                                                "includeDirectories", Map.of(
                                                                                                "type", "boolean",
                                                                                                "description",
                                                                                                "Whether to include directories in the results (default: true)")),
                                                                "required", List.of("directoryPath")))));

                // Upload directory function
                functions.add(new FunctionDefinition("upload_directory")
                                .setDescription("Uploads all files from a local directory to blob storage")
                                .setParameters(BinaryData.fromObject(
                                                Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "directoryPath", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Path to the directory containing files to upload (e.g., 'Downloads/MassDownloader')"),
                                                                                "pattern", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Optional regex pattern to filter files (e.g., '.*\\.pdf' for PDF files only)"),
                                                                                "recursive", Map.of(
                                                                                                "type", "boolean",
                                                                                                "description",
                                                                                                "Whether to upload files from subdirectories (default: false)"),
                                                                                "blobPrefix", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Optional prefix to add to all uploaded blob names (e.g., 'uploads/')")),
                                                                "required", List.of("directoryPath")))));

                return functions;
        }
}