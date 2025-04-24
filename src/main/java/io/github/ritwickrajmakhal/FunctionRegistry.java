package io.github.ritwickrajmakhal;

import com.azure.ai.openai.models.FunctionDefinition;
import com.azure.core.util.BinaryData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FunctionRegistry {

    /**
     * Returns a list of function definitions for blob management.
     */
    public static List<FunctionDefinition> getBlobFunctionDefinitions() {
        List<FunctionDefinition> functions = new ArrayList<>();

        // List blobs function
        functions.add(new FunctionDefinition("list_blobs")
            .setDescription("Lists all blobs in the container with metadata")
            .setParameters(BinaryData.fromObject(
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "prefix", Map.of(
                            "type", "string",
                            "description", "Optional prefix to filter blobs"
                        ),
                        "maxResults", Map.of(
                            "type", "integer",
                            "description", "Maximum number of results to return"
                        )
                    ),
                    "required", List.of()
                )
            ))
        );

        // Upload file function
        functions.add(new FunctionDefinition("upload_file")
            .setDescription("Uploads a file to the container")
            .setParameters(BinaryData.fromObject(
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "filePath", Map.of(
                            "type", "string",
                            "description", "Path to the file to upload (local or URL)"
                        )
                    ),
                    "required", List.of("filePath")
                )
            ))
        );

        // Download blob function
        functions.add(new FunctionDefinition("download_blob")
            .setDescription("Downloads a blob from the container")
            .setParameters(BinaryData.fromObject(
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "blobName", Map.of(
                            "type", "string",
                            "description", "Name of the blob to download"
                        ),
                        "destinationPath", Map.of(
                            "type", "string",
                            "description", "Path to save the downloaded blob"
                        )
                    ),
                    "required", List.of("blobName", "destinationPath")
                )
            ))
        );

        // Delete blob function
        functions.add(new FunctionDefinition("delete_blob")
            .setDescription("Deletes a blob from the container")
            .setParameters(BinaryData.fromObject(
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "blobName", Map.of(
                            "type", "string",
                            "description", "Name of the blob to delete"
                        )
                    ),
                    "required", List.of("blobName")
                )
            ))
        );

        // Rename blob function
        functions.add(new FunctionDefinition("rename_blob")
            .setDescription("Renames or moves a blob within the container")
            .setParameters(BinaryData.fromObject(
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "sourceBlobName", Map.of(
                            "type", "string",
                            "description", "Name of the source blob"
                        ),
                        "destinationBlobName", Map.of(
                            "type", "string",
                            "description", "New name or path for the blob"
                        )
                    ),
                    "required", List.of("sourceBlobName", "destinationBlobName")
                )
            ))
        );

        functions.add(new FunctionDefinition("search_blobs")
            .setDescription("Search for blobs in Azure Storage by query")
            .setParameters(BinaryData.fromObject(
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of(
                            "type", "string",
                            "description", "Query to search for blobs"
                        ),
                        "maxResults", Map.of(
                            "type", "integer",
                            "description", "Maximum number of results to return (default: 5)"
                        ),
                        "includeContent", Map.of(
                            "type", "boolean",
                            "description", "Whether to include content summaries (default: false)"
                        )
                    ),
                    "required", List.of("query", "maxResults")
                )
            ))
        );

        return functions;
    }
}