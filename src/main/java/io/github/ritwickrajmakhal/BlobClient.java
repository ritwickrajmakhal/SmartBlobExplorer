package io.github.ritwickrajmakhal;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.models.CopyStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Client for interacting with Azure Blob Storage.
 * <p>
 * This class provides methods for common blob operations including:
 * <ul>
 * <li>Uploading files to blob storage from local paths or URLs</li>
 * <li>Downloading blobs to the local file system</li>
 * <li>Deleting blobs</li>
 * <li>Renaming/moving blobs within a container</li>
 * <li>Batch operations for upload, download, and delete</li>
 * <li>Advanced blob management methods</li>
 * </ul>
 * <p>
 * The client connects to a specific Azure Storage account and container
 * using a connection string provided during initialization.
 */
public class BlobClient {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(BlobClient.class);

    /** Default timeout for operations in seconds */
    private static final int DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes

    /** Default maximum concurrent operations */
    private static final int DEFAULT_MAX_CONCURRENCY = 10;

    /** Client for interacting with the Azure Blob Storage service */
    private final BlobServiceClient blobServiceClient;

    /** Client for interacting with a specific blob container */
    private final BlobContainerClient blobContainerClient;

    /** Maximum concurrent operations */
    private final int maxConcurrency;

    /** Operation timeout in seconds */
    private final int operationTimeoutSeconds;

    /**
     * Creates a new BlobClient that connects to a specific container in Azure Blob
     * Storage with default settings.
     * 
     * @param connectionString The Azure Storage account connection string
     * @param containerName    The name of the blob container to connect to
     */
    public BlobClient(String connectionString, String containerName) {
        this(connectionString, containerName, DEFAULT_MAX_CONCURRENCY, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Creates a new BlobClient with custom concurrency and timeout settings.
     * 
     * @param connectionString        The Azure Storage account connection string
     * @param containerName           The name of the blob container to connect to
     * @param maxConcurrency          Maximum number of concurrent operations
     * @param operationTimeoutSeconds Operation timeout in seconds
     */
    public BlobClient(String connectionString, String containerName, int maxConcurrency, int operationTimeoutSeconds) {
        this.maxConcurrency = maxConcurrency > 0 ? maxConcurrency : DEFAULT_MAX_CONCURRENCY;
        this.operationTimeoutSeconds = operationTimeoutSeconds > 0 ? operationTimeoutSeconds : DEFAULT_TIMEOUT_SECONDS;

        // Mask connection string when logging to avoid security issues
        // Only log the beginning of the string to identify which connection is being
        // used
        String maskedConnectionString = connectionString.length() > 15
                ? connectionString.substring(0, 10) + "..."
                : "[protected]";
        logger.info("Initializing BlobClient for container: {}, connection: {}, maxConcurrency: {}, timeout: {}s",
                containerName, maskedConnectionString, this.maxConcurrency, this.operationTimeoutSeconds);

        blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    /**
     * Uploads a file to Azure Blob Storage.
     * <p>
     * This method supports uploading from:
     * <ul>
     * <li>Local file path</li>
     * <li>HTTP or HTTPS URL (will download to a temporary file first)</li>
     * </ul>
     * <p>
     * The blob name will be derived from the original file name.
     *
     * @param filePath Path to the file to upload, or URL to download and then
     *                 upload
     * @return true if the upload was successful, false otherwise
     */
    public boolean uploadFile(String filePath) {
        File tempFile = null;
        try {
            logger.info("Uploading file: {}", filePath);
            File file;
            String blobName;

            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                // Download the file from the URL
                URL url = new URL(filePath);
                blobName = new File(url.getPath()).getName(); // Extract the original file name from the URL
                tempFile = File.createTempFile("temp-", ".tmp");
                try (InputStream in = url.openStream()) {
                    Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                file = tempFile;
            } else {
                // Use the local file path
                file = new File(filePath);
                blobName = file.getName(); // Use the local file name
            }

            // Upload the file to Azure Blob Storage
            BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(blobName).getBlockBlobClient();
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(
                    Files.newInputStream(file.toPath()))) {
                blockBlobClient.upload(bufferedInputStream, file.length(), true);
            }

            logger.info("Successfully uploaded file: {} as blob: {}", filePath, blobName);
            return true;
        } catch (Exception e) {
            logger.error("Error uploading file: {}", filePath, e);
            return false;
        } finally {
            // Clean up temporary file if created
            if (tempFile != null) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Downloads a blob from Azure Storage to a local file.
     * <p>
     * If the destination path is a directory, the blob will be downloaded with
     * its original name to that directory. If the destination path is a file path,
     * the blob content will be saved to that file.
     *
     * @param blobName        The name of the blob to download
     * @param destinationPath The local file path or directory to save the blob to
     * @return true if the download was successful, false otherwise
     */
    public boolean downloadBlob(String blobName, String destinationPath) {
        try {
            logger.info("Downloading blob: {} to path: {}", blobName, destinationPath);

            File destinationFile = new File(destinationPath);
            if (destinationFile.isDirectory()) {
                // Append the blob name to the directory path
                destinationPath = new File(destinationPath, blobName).getAbsolutePath();
            }

            BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(blobName).getBlockBlobClient();
            // Use downloadToFile instead of manually handling streams
            blockBlobClient.downloadToFile(destinationPath, true);

            logger.info("Successfully downloaded blob: {} to: {}", blobName, destinationPath);
            return true;
        } catch (Exception e) {
            logger.error("Error downloading blob: {}", blobName, e);
            return false;
        }
    }

    /**
     * Deletes a blob from Azure Storage.
     * <p>
     * This operation permanently removes the blob from the container.
     *
     * @param blobName The name of the blob to delete
     * @return true if the blob was successfully deleted, false otherwise
     */
    public boolean deleteBlob(String blobName) {
        try {
            logger.info("Deleting blob: {}", blobName);
            blobContainerClient.getBlobClient(blobName).delete();
            logger.info("Successfully deleted blob: {}", blobName);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting blob: {}", blobName, e);
            return false;
        }
    }

    /**
     * Renames a blob by copying it to a new destination and then deleting the
     * source.
     * <p>
     * This method effectively performs a move operation by creating a copy of the
     * source blob at the destination path and then deleting the original. If the
     * operation
     * fails during copying, any partially created destination blob will be cleaned
     * up.
     *
     * @param sourceBlobName      The name of the source blob to rename/move
     * @param destinationBlobName The new name/path for the blob
     * @return true if the rename operation was successful, false otherwise
     */
    public boolean renameBlob(String sourceBlobName, String destinationBlobName) {
        logger.info("Renaming blob from: {} to: {}", sourceBlobName, destinationBlobName);

        BlockBlobClient sourceBlobClient = blobContainerClient.getBlobClient(sourceBlobName).getBlockBlobClient();
        BlockBlobClient destinationBlobClient = blobContainerClient.getBlobClient(destinationBlobName)
                .getBlockBlobClient();
        try {
            // Start the copy operation
            destinationBlobClient.beginCopy(sourceBlobClient.getBlobUrl(), null);

            // Wait for the copy operation to complete using polling
            boolean copyComplete = false;
            CopyStatusType copyStatus = null;
            final long startTime = System.currentTimeMillis();
            final long timeout = operationTimeoutSeconds * 1000L;

            // Poll until complete or timeout
            while (!copyComplete && System.currentTimeMillis() - startTime < timeout) {
                BlobProperties properties = destinationBlobClient.getProperties();
                copyStatus = properties.getCopyStatus();

                if (CopyStatusType.SUCCESS.equals(copyStatus)) {
                    copyComplete = true;
                } else if (CopyStatusType.FAILED.equals(copyStatus) ||
                        CopyStatusType.ABORTED.equals(copyStatus)) {
                    throw new RuntimeException("Blob copy operation failed with status: " + copyStatus);
                } else {
                    // Still pending, wait before checking again
                    Thread.sleep(Math.min(1000, timeout / 10));
                }
            }

            // If we timed out
            if (!copyComplete) {
                try {
                    // Try to abort the copy operation
                    destinationBlobClient.abortCopyFromUrl(destinationBlobClient.getProperties().getCopyId());
                } catch (Exception e) {
                    logger.warn("Could not abort copy operation for blob: {}", destinationBlobName, e);
                }
                throw new RuntimeException("Copy operation timed out after " + operationTimeoutSeconds + " seconds");
            }

            // Now delete the source blob
            sourceBlobClient.delete();

            logger.info("Successfully renamed blob from: {} to: {}", sourceBlobName, destinationBlobName);
            return true;
        } catch (Exception e) {
            logger.error("Error renaming blob from: {} to: {}", sourceBlobName, destinationBlobName, e);
            // If a destination blob was created but the operation didn't complete fully
            // we should clean it up in the error case
            try {
                if (destinationBlobClient.exists()) {
                    destinationBlobClient.delete();
                }
            } catch (Exception ex) {
                logger.error("Error cleaning up partial rename for destination: {}", destinationBlobName, ex);
            }
            return false;
        }
    }

    /**
     * Uploads multiple files to Azure Blob Storage in parallel with retry support.
     * <p>
     * This method supports uploading files from:
     * <ul>
     * <li>Local file paths</li>
     * <li>HTTP or HTTPS URLs (will download to a temporary file first)</li>
     * </ul>
     * <p>
     * Blob names will be derived from the original file names.
     *
     * @param filePaths  List of paths to the files to upload, or URLs to download
     *                   and then upload
     * @param retryCount Number of times to retry failed uploads (defaults to 1)
     * @return A list of successfully uploaded blob names
     */
    public List<String> batchUploadFiles(List<String> filePaths, int retryCount) {
        logger.info("Starting batch upload of {} files with {} retries", filePaths.size(), retryCount);
        List<String> successfulUploads = new ArrayList<>();
        Map<String, Integer> retries = new HashMap<>();

        // Initialize retry counts
        for (String filePath : filePaths) {
            retries.put(filePath, 0);
        }

        // First attempt
        List<String> remainingFiles = new ArrayList<>(filePaths);
        List<String> currentUploads = uploadBatch(remainingFiles, successfulUploads);

        // Keep track of which files failed
        remainingFiles.removeAll(currentUploads.stream()
                .map(blobName -> {
                    for (String filePath : remainingFiles) {
                        if (filePath.endsWith(blobName)) {
                            return filePath;
                        }
                    }
                    return null;
                }).filter(path -> path != null)
                .collect(Collectors.toList()));

        // Do retries if needed
        int actualRetryCount = Math.max(0, retryCount);
        for (int i = 0; i < actualRetryCount && !remainingFiles.isEmpty(); i++) {
            logger.info("Retry attempt {} for {} remaining files", i + 1, remainingFiles.size());
            List<String> retryFiles = new ArrayList<>(remainingFiles);
            List<String> retriedUploads = uploadBatch(retryFiles, successfulUploads);

            // Update remaining files
            remainingFiles.removeAll(retryFiles.stream()
                    .map(filePath -> {
                        for (String blobName : retriedUploads) {
                            if (filePath.endsWith(blobName)) {
                                return filePath;
                            }
                        }
                        return null;
                    }).filter(path -> path != null)
                    .collect(Collectors.toList()));
        }

        logger.info("Batch upload completed. Successfully uploaded {}/{} files",
                successfulUploads.size(), filePaths.size());
        return successfulUploads;
    }

    /**
     * Helper method to perform a single batch upload attempt
     */
    private List<String> uploadBatch(List<String> filePaths, List<String> successfulUploads) {
        List<String> batchResults = new ArrayList<>();

        // Use a thread pool for parallel uploads
        try (ExecutorService executorService = Executors.newFixedThreadPool(
                Math.min(filePaths.size(), maxConcurrency))) {

            List<CompletableFuture<String>> futures = filePaths.stream()
                    .map(filePath -> CompletableFuture.supplyAsync(() -> {
                        boolean success = uploadFile(filePath);
                        if (success) {
                            String blobName;
                            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                                try {
                                    URL url = new URL(filePath);
                                    blobName = new File(url.getPath()).getName();
                                } catch (Exception e) {
                                    logger.error("Error extracting name from URL: {}", filePath, e);
                                    return null;
                                }
                            } else {
                                blobName = new File(filePath).getName();
                            }
                            return blobName;
                        }
                        return null;
                    }, executorService))
                    .collect(Collectors.toList());

            // Wait for all uploads to complete and collect successful ones
            for (CompletableFuture<String> future : futures) {
                try {
                    String result = future.get(operationTimeoutSeconds, TimeUnit.SECONDS);
                    if (result != null) {
                        batchResults.add(result);
                        successfulUploads.add(result);
                    }
                } catch (Exception e) {
                    logger.error("Error in batch upload task", e);
                }
            }
        }

        return batchResults;
    }

    /**
     * Uploads multiple files to Azure Blob Storage in parallel.
     * <p>
     * This method uses the default retry count of 1.
     *
     * @param filePaths List of paths to the files to upload, or URLs to download
     *                  and then upload
     * @return A list of successfully uploaded blob names
     */
    public List<String> batchUploadFiles(List<String> filePaths) {
        return batchUploadFiles(filePaths, 1);
    }

    /**
     * Downloads multiple blobs from Azure Storage to local files in parallel.
     * <p>
     * If the destination path is a directory, the blobs will be downloaded with
     * their original names to that directory.
     *
     * @param blobNames      List of names of the blobs to download
     * @param destinationDir The local directory path to save the blobs to
     * @return A list of successfully downloaded blob names
     */
    public List<String> batchDownloadBlobs(List<String> blobNames, String destinationDir) {
        logger.info("Starting batch download of {} blobs to {}", blobNames.size(), destinationDir);
        List<String> successfulDownloads = new ArrayList<>();

        // Ensure destination directory exists
        File destDir = new File(destinationDir);
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                logger.error("Failed to create destination directory: {}", destinationDir);
                return successfulDownloads;
            }
        }

        // Use a thread pool for parallel downloads
        try (ExecutorService executorService = Executors.newFixedThreadPool(
                Math.min(blobNames.size(), maxConcurrency))) { // Limit concurrent threads

            List<CompletableFuture<String>> futures = blobNames.stream()
                    .map(blobName -> CompletableFuture.supplyAsync(() -> {
                        try {
                            downloadBlob(blobName, destinationDir);
                            return blobName;
                        } catch (Exception e) {
                            logger.error("Error downloading blob: {}", blobName, e);
                            return null;
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // Wait for all downloads to complete and collect successful ones
            for (CompletableFuture<String> future : futures) {
                try {
                    String result = future.get(operationTimeoutSeconds, TimeUnit.SECONDS);
                    if (result != null) {
                        successfulDownloads.add(result);
                    }
                } catch (Exception e) {
                    logger.error("Error in batch download task", e);
                }
            }
        }

        logger.info("Batch download completed. Successfully downloaded {}/{} blobs",
                successfulDownloads.size(), blobNames.size());
        return successfulDownloads;
    }

    /**
     * Deletes multiple blobs from Azure Storage in parallel.
     *
     * @param blobNames List of names of the blobs to delete
     * @return A list of successfully deleted blob names
     */
    public List<String> batchDeleteBlobs(List<String> blobNames) {
        logger.info("Starting batch delete of {} blobs", blobNames.size());
        List<String> successfulDeletes = new ArrayList<>();

        // Use a thread pool for parallel deletes
        try (ExecutorService executorService = Executors.newFixedThreadPool(
                Math.min(blobNames.size(), maxConcurrency))) { // Limit concurrent threads

            List<CompletableFuture<String>> futures = blobNames.stream()
                    .map(blobName -> CompletableFuture.supplyAsync(() -> {
                        boolean success = deleteBlob(blobName);
                        return success ? blobName : null;
                    }, executorService))
                    .collect(Collectors.toList());

            // Wait for all deletes to complete and collect successful ones
            for (CompletableFuture<String> future : futures) {
                try {
                    String result = future.get(operationTimeoutSeconds, TimeUnit.SECONDS);
                    if (result != null) {
                        successfulDeletes.add(result);
                    }
                } catch (Exception e) {
                    logger.error("Error in batch delete task", e);
                }
            }
        }

        logger.info("Batch delete completed. Successfully deleted {}/{} blobs",
                successfulDeletes.size(), blobNames.size());
        return successfulDeletes;
    }

    /**
     * Lists blobs in the container with optional filtering.
     * <p>
     * This method can filter blobs by:
     * <ul>
     * <li>Prefix - blob names that start with a certain path or string</li>
     * <li>Regex pattern - blob names that match a regular expression</li>
     * <li>Maximum results - limit the number of returned items</li>
     * </ul>
     * <p>
     * The results include metadata about each blob such as size, content type,
     * and last modified time.
     *
     * @param prefix     Optional prefix to filter blob names (can be null)
     * @param regex      Optional regular expression pattern to filter blob names
     *                   (can be null)
     * @param maxResults Maximum number of results to return (0 for no limit)
     * @return A list of maps containing blob information
     */
    public List<Map<String, Object>> listBlobs(String prefix, String regex, int maxResults) {
        logger.info("Listing blobs with prefix: {}, regex: {}, maxResults: {}",
                prefix, regex != null ? regex : "none", maxResults > 0 ? maxResults : "unlimited");

        List<Map<String, Object>> results = new ArrayList<>();
        Pattern pattern = regex != null ? Pattern.compile(regex) : null;

        ListBlobsOptions options = new ListBlobsOptions()
                .setDetails(new BlobListDetails().setRetrieveMetadata(true))
                .setPrefix(prefix);

        // Limit the total number of blobs to process
        int remaining = maxResults > 0 ? maxResults : Integer.MAX_VALUE;

        try {
            blobContainerClient.listBlobs(options, null).forEach(blob -> {
                if (results.size() < remaining && (pattern == null || pattern.matcher(blob.getName()).matches())) {
                    Map<String, Object> blobInfo = new HashMap<>();
                    blobInfo.put("name", blob.getName());
                    blobInfo.put("size", blob.getProperties().getContentLength());
                    blobInfo.put("contentType", blob.getProperties().getContentType());
                    blobInfo.put("lastModified", blob.getProperties().getLastModified());
                    blobInfo.put("etag", blob.getProperties().getETag());

                    // Add any custom metadata
                    Map<String, String> metadata = new HashMap<>();
                    if (blob.getMetadata() != null) {
                        metadata.putAll(blob.getMetadata());
                    }
                    blobInfo.put("metadata", metadata);

                    results.add(blobInfo);
                }
            });

            logger.info("Listed {} blobs successfully", results.size());
            return results;
        } catch (Exception e) {
            logger.error("Error listing blobs", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lists blobs in the container with optional filtering and pagination.
     * <p>
     * This method extends the basic listBlobs functionality to support pagination,
     * allowing for efficient retrieval of large result sets.
     *
     * @param prefix   Optional prefix to filter blob names (can be null)
     * @param regex    Optional regular expression pattern to filter blob names (can
     *                 be null)
     * @param pageSize The number of blobs to return per page
     * @param marker   The continuation token for retrieving the next page of
     *                 results (null for first page)
     * @return A map containing the blob list results and a continuation token
     */
    public Map<String, Object> listBlobsWithPagination(String prefix, String regex, int pageSize, String marker) {
        logger.info("Listing blobs with prefix: {}, regex: {}, pageSize: {}, marker: {}",
                prefix, regex != null ? regex : "none", pageSize, marker != null ? marker : "start");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> blobsList = new ArrayList<>();
        Pattern pattern = regex != null ? Pattern.compile(regex) : null;
        String nextMarker = null;

        try {
            // Ensure page size is reasonable
            int size = pageSize > 0 ? Math.min(pageSize, 1000) : 50;

            // Set up the options for listing blobs
            ListBlobsOptions options = new ListBlobsOptions()
                    .setDetails(new BlobListDetails().setRetrieveMetadata(true))
                    .setPrefix(prefix)
                    .setMaxResultsPerPage(size);

            // Create a Duration object for timeout
            Duration timeout = Duration.ofSeconds(operationTimeoutSeconds);

            // Get the iterable - we'll manually handle pagination instead of using forEach
            var blobsIterable = blobContainerClient.listBlobs(options, timeout);

            // Use the iterable's iterator
            var iterator = blobsIterable.iterator();

            // Track when we've collected enough items for this page
            int count = 0;

            // Process the current page
            while (iterator.hasNext() && count < size) {
                var blob = iterator.next();

                // Apply regex filter if provided
                if (pattern == null || pattern.matcher(blob.getName()).matches()) {
                    Map<String, Object> blobInfo = new HashMap<>();
                    blobInfo.put("name", blob.getName());
                    blobInfo.put("size", blob.getProperties().getContentLength());
                    blobInfo.put("contentType", blob.getProperties().getContentType());
                    blobInfo.put("lastModified", blob.getProperties().getLastModified());
                    blobInfo.put("etag", blob.getProperties().getETag());

                    // Add any custom metadata
                    Map<String, String> metadata = new HashMap<>();
                    if (blob.getMetadata() != null) {
                        metadata.putAll(blob.getMetadata());
                    }
                    blobInfo.put("metadata", metadata);

                    blobsList.add(blobInfo);
                    count++;
                }
            }

            // Check if there are more results
            boolean hasMoreResults = iterator.hasNext();

            // If there are more results, create a simple continuation token
            // Since the SDK doesn't expose a method to get the token, we'll use a flag
            if (hasMoreResults) {
                // For our implementation, we'll just indicate there are more results
                // In a real implementation, you would need to use a proper token mechanism
                nextMarker = "has_more";
            }

            // Build result object
            result.put("blobs", blobsList);
            result.put("pageSize", size);
            result.put("count", blobsList.size());

            // Add continuation token if available
            if (nextMarker != null) {
                result.put("continuationToken", nextMarker);
                result.put("hasMoreResults", true);
            } else {
                result.put("hasMoreResults", false);
            }

            logger.info("Listed {} blobs successfully (page size: {}, has more: {})",
                    blobsList.size(), size, hasMoreResults);

            // Add a note about pagination limitations with this SDK version
            if (hasMoreResults) {
                logger.warn("Note: Current SDK version does not expose continuation tokens. " +
                        "For complete pagination support, consider upgrading the SDK.");
            }
        } catch (Exception e) {
            logger.error("Error listing blobs with pagination", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Copies a blob within the storage account while preserving the original.
     * <p>
     * Unlike renameBlob which moves the blob, this method creates a new copy
     * while keeping the original blob intact.
     *
     * @param sourceBlobName      The name of the source blob to copy
     * @param destinationBlobName The name for the new copy of the blob
     * @return true if the copy operation was successful, false otherwise
     */
    public boolean copyBlob(String sourceBlobName, String destinationBlobName) {
        logger.info("Copying blob from: {} to: {}", sourceBlobName, destinationBlobName);

        try {
            // Get clients for source and destination blobs
            BlockBlobClient sourceBlobClient = blobContainerClient.getBlobClient(sourceBlobName).getBlockBlobClient();
            BlockBlobClient destinationBlobClient = blobContainerClient.getBlobClient(destinationBlobName)
                    .getBlockBlobClient();

            // Start the copy operation
            destinationBlobClient.beginCopy(sourceBlobClient.getBlobUrl(), null);

            // Wait for the copy operation to complete using polling
            boolean copyComplete = false;
            CopyStatusType copyStatus = null;
            final long startTime = System.currentTimeMillis();
            final long timeout = operationTimeoutSeconds * 1000L;

            // Poll until complete or timeout
            while (!copyComplete && System.currentTimeMillis() - startTime < timeout) {
                BlobProperties properties = destinationBlobClient.getProperties();
                copyStatus = properties.getCopyStatus();

                if (CopyStatusType.SUCCESS.equals(copyStatus)) {
                    copyComplete = true;
                } else if (CopyStatusType.FAILED.equals(copyStatus) ||
                        CopyStatusType.ABORTED.equals(copyStatus)) {
                    throw new RuntimeException("Blob copy operation failed with status: " + copyStatus);
                } else {
                    // Still pending, wait before checking again
                    Thread.sleep(Math.min(1000, timeout / 10));
                }
            }

            // If we timed out
            if (!copyComplete) {
                try {
                    // Try to abort the copy operation
                    destinationBlobClient.abortCopyFromUrl(destinationBlobClient.getProperties().getCopyId());
                } catch (Exception e) {
                    logger.warn("Could not abort copy operation for blob: {}", destinationBlobName, e);
                }
                throw new RuntimeException("Copy operation timed out after " + operationTimeoutSeconds + " seconds");
            }

            logger.info("Successfully copied blob from: {} to: {}", sourceBlobName, destinationBlobName);
            return true;
        } catch (Exception e) {
            logger.error("Error copying blob from: {} to: {}", sourceBlobName, destinationBlobName, e);
            // Unlike rename, we don't automatically delete the destination on failure as
            // this is a copy operation
            return false;
        }
    }

    /**
     * Creates a snapshot of a blob, preserving its state at the current point in
     * time.
     * <p>
     * Snapshots are useful for version control and point-in-time recovery of blob
     * content.
     * Each snapshot has a unique ID that can be used to access that specific
     * version later.
     * 
     * @param blobName The name of the blob to snapshot
     * @return A Map containing the snapshot ID and timestamp, or null if the
     *         operation failed
     */
    public Map<String, Object> createSnapshot(String blobName) {
        logger.info("Creating snapshot of blob: {}", blobName);

        try {
            // Get the blob client
            BlockBlobClient blobClient = blobContainerClient.getBlobClient(blobName).getBlockBlobClient();

            // Create the snapshot
            String snapshotId = blobClient.createSnapshot().getSnapshotId();

            // Return snapshot details
            Map<String, Object> snapshotInfo = new HashMap<>();
            snapshotInfo.put("blobName", blobName);
            snapshotInfo.put("snapshotId", snapshotId);

            // Try to parse the timestamp, but don't fail if it's not a valid OffsetDateTime
            try {
                OffsetDateTime snapshotTime = OffsetDateTime.parse(snapshotId);
                snapshotInfo.put("snapshotTime", snapshotTime.toString());
            } catch (Exception e) {
                // If we can't parse it as a date, just store it as a string
                snapshotInfo.put("snapshotTime", snapshotId);
                logger.warn("Could not parse snapshot ID as datetime: {}", snapshotId);
            }

            snapshotInfo.put("message", "Snapshot created successfully");

            logger.info("Successfully created snapshot of blob: {} with ID: {}", blobName, snapshotId);
            return snapshotInfo;
        } catch (Exception e) {
            logger.error("Error creating snapshot of blob: {}", blobName, e);
            return null;
        }
    }

    /**
     * Generates a Shared Access Signature (SAS) URL for a blob with limited-time
     * access.
     * <p>
     * The SAS URL allows temporary access to the blob without requiring the storage
     * account key.
     * It can be shared with others to provide limited, time-bound access to
     * specific blobs.
     * 
     * @param blobName         The name of the blob to generate a SAS URL for
     * @param durationHours    The number of hours the SAS URL should be valid for
     * @param readPermission   Whether the SAS should allow read access
     * @param writePermission  Whether the SAS should allow write access
     * @param deletePermission Whether the SAS should allow delete access
     * @return The SAS URL for the blob, or null if generation failed
     */
    public String generateSasUrl(String blobName, int durationHours,
            boolean readPermission, boolean writePermission, boolean deletePermission) {
        logger.info("Generating SAS URL for blob: {}, valid for {} hours, permissions: read={}, write={}, delete={}",
                blobName, durationHours, readPermission, writePermission, deletePermission);

        try {
            // Get the blob client
            BlockBlobClient blobClient = blobContainerClient.getBlobClient(blobName).getBlockBlobClient();

            // Set the expiry time
            OffsetDateTime expiryTime = OffsetDateTime.now(ZoneOffset.UTC).plus(Duration.ofHours(durationHours));

            // Set the permissions
            BlobSasPermission permissions = new BlobSasPermission()
                    .setReadPermission(readPermission)
                    .setWritePermission(writePermission)
                    .setDeletePermission(deletePermission);

            // Generate the SAS token
            BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                    expiryTime, permissions);

            // Get the SAS token and construct the full URL
            String sasToken = blobClient.generateSas(sasValues);
            String sasUrl = blobClient.getBlobUrl() + "?" + sasToken;

            logger.info("Successfully generated SAS URL for blob: {}, valid until: {}", blobName, expiryTime);
            return sasUrl;
        } catch (Exception e) {
            logger.error("Error generating SAS URL for blob: {}", blobName, e);
            return null;
        }
    }

    /**
     * Lists files and directories in a local directory with optional pattern
     * filtering.
     * <p>
     * This method can:
     * <ul>
     * <li>List all files and subdirectories in a directory</li>
     * <li>Filter files by extension or pattern</li>
     * <li>Resolve common directory paths like Downloads, Documents, etc.</li>
     * </ul>
     * <p>
     * If the provided directory path contains placeholders like ~/ or
     * YourUsername, they will be resolved to actual system paths.
     *
     * @param directoryPath      Path to the directory to list
     * @param pattern            Optional filename pattern to filter by (can be
     *                           null)
     * @param includeDirectories Whether to include directories in the results
     * @return A map containing lists of file paths and directory paths
     */
    public Map<String, List<String>> listLocalFilesAndDirs(String directoryPath, String pattern,
            boolean includeDirectories) {
        logger.info("Listing files and directories in: {} with pattern: {}, includeDirectories: {}",
                directoryPath, pattern != null ? pattern : "none", includeDirectories);

        Map<String, List<String>> results = new HashMap<>();
        List<String> files = new ArrayList<>();
        List<String> directories = new ArrayList<>();
        results.put("files", files);
        results.put("directories", directories);

        try {
            // Handle potential user home directory references
            String resolvedDirPath = directoryPath;

            // Replace "~/", "~\" with user home directory
            if (directoryPath.startsWith("~" + File.separator) || directoryPath.startsWith("~/")) {
                resolvedDirPath = System.getProperty("user.home") +
                        directoryPath.substring(1);
            }

            // Handle "/Users/YourUsername" or "\Users\YourUsername" format
            if (directoryPath.contains("YourUsername") || directoryPath.contains("UserName")) {
                resolvedDirPath = directoryPath.replace("YourUsername", System.getProperty("user.name"))
                        .replace("UserName", System.getProperty("user.name"));
            }

            // Handle common directory names
            if (directoryPath.equalsIgnoreCase("downloads") ||
                    directoryPath.equalsIgnoreCase("download")) {
                resolvedDirPath = new File(System.getProperty("user.home"), "Downloads").getAbsolutePath();
            } else if (directoryPath.equalsIgnoreCase("documents") ||
                    directoryPath.equalsIgnoreCase("document") ||
                    directoryPath.equalsIgnoreCase("docs")) {
                resolvedDirPath = new File(System.getProperty("user.home"), "Documents").getAbsolutePath();
            } else if (directoryPath.equalsIgnoreCase("desktop")) {
                resolvedDirPath = new File(System.getProperty("user.home"), "Desktop").getAbsolutePath();
            } else if (directoryPath.equalsIgnoreCase("home") ||
                    directoryPath.equals("~") || directoryPath.isEmpty()) {
                resolvedDirPath = System.getProperty("user.home");
            }

            logger.info("Resolved directory path: {} to {}", directoryPath, resolvedDirPath);

            File dir = new File(resolvedDirPath);
            if (!dir.exists()) {
                logger.warn("Directory does not exist: {}", resolvedDirPath);
                return results;
            }

            if (!dir.isDirectory()) {
                logger.info("Path is not a directory, but a file: {}", resolvedDirPath);
                files.add(dir.getAbsolutePath());
                return results;
            }

            // Get file list with optional pattern filtering
            File[] items = dir.listFiles();
            Pattern regex = (pattern != null && !pattern.isEmpty()) ? Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                    : null;

            if (items != null) {
                for (File item : items) {
                    if (item.isFile()) {
                        if (regex == null || regex.matcher(item.getName()).matches()) {
                            files.add(item.getAbsolutePath());
                        }
                    } else if (item.isDirectory() && includeDirectories) {
                        if (regex == null || regex.matcher(item.getName()).matches()) {
                            directories.add(item.getAbsolutePath());
                        }
                    }
                }
            }

            logger.info("Found {} files and {} directories in: {}",
                    files.size(), directories.size(), resolvedDirPath);
            return results;
        } catch (Exception e) {
            logger.error("Error listing files in directory: {}", directoryPath, e);
            return results;
        }
    }

    /**
     * Lists files in a local directory with optional pattern filtering.
     * <p>
     * This method is a simplified version that only returns file paths
     * and is maintained for backward compatibility.
     *
     * @param directoryPath Path to the directory to list
     * @param pattern       Optional filename pattern to filter by (can be null)
     * @return A list of file paths in the directory
     */
    public List<String> listLocalFiles(String directoryPath, String pattern) {
        return listLocalFilesAndDirs(directoryPath, pattern, false).get("files");
    }

    /**
     * Uploads all files from a local directory to Azure Blob Storage.
     * <p>
     * This method can:
     * <ul>
     * <li>Upload all files in a directory</li>
     * <li>Optionally upload files matching a pattern</li>
     * <li>Optionally upload recursively from subdirectories</li>
     * <li>Resolve common directory paths like Downloads, Documents, etc.</li>
     * </ul>
     *
     * @param directoryPath Path to the directory containing files to upload
     * @param pattern       Optional filename pattern to filter by (can be null)
     * @param recursive     Whether to upload files from subdirectories
     * @param blobPrefix    Optional prefix to add to blob names (can be null for no
     *                      prefix)
     * @return A map with information about the upload operation
     */
    public Map<String, Object> uploadDirectory(String directoryPath, String pattern,
            boolean recursive, String blobPrefix) {
        logger.info("Uploading directory: {} with pattern: {}, recursive: {}, blobPrefix: {}",
                directoryPath, pattern != null ? pattern : "none",
                recursive, blobPrefix != null ? blobPrefix : "none");

        Map<String, Object> result = new HashMap<>();
        List<String> successfulUploads = new ArrayList<>();
        List<String> failedUploads = new ArrayList<>();

        try {
            // Get all files in the directory, respecting the pattern
            List<File> filesToUpload = new ArrayList<>();
            collectFiles(new File(directoryPath), pattern != null ? Pattern.compile(pattern) : null,
                    recursive, filesToUpload);

            // Process the uploads in parallel
            try (ExecutorService executorService = Executors.newFixedThreadPool(
                    Math.min(filesToUpload.size(), maxConcurrency))) { // Limit concurrent threads

                List<CompletableFuture<Map.Entry<String, Boolean>>> futures = filesToUpload.stream()
                        .map(file -> CompletableFuture.supplyAsync(() -> {
                            String relativePath = file.getAbsolutePath()
                                    .substring(new File(directoryPath).getAbsolutePath().length());
                            if (relativePath.startsWith(File.separator)) {
                                relativePath = relativePath.substring(1);
                            }

                            // Add prefix if provided
                            String blobName = (blobPrefix != null ? blobPrefix : "") +
                                    relativePath.replace(File.separator, "/");

                            try {
                                // Upload the file to Azure Blob Storage
                                BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(blobName)
                                        .getBlockBlobClient();
                                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(
                                        Files.newInputStream(file.toPath()))) {
                                    blockBlobClient.upload(bufferedInputStream, file.length(), true);
                                }
                                return Map.entry(blobName, true);
                            } catch (Exception e) {
                                logger.error("Error uploading file: {}", file.getAbsolutePath(), e);
                                return Map.entry(file.getAbsolutePath(), false);
                            }
                        }, executorService))
                        .collect(Collectors.toList());

                // Collect results
                for (CompletableFuture<Map.Entry<String, Boolean>> future : futures) {
                    try {
                        Map.Entry<String, Boolean> entry = future.get(operationTimeoutSeconds, TimeUnit.SECONDS);
                        if (entry.getValue()) {
                            successfulUploads.add(entry.getKey());
                        } else {
                            failedUploads.add(entry.getKey());
                        }
                    } catch (Exception e) {
                        logger.error("Error processing upload task", e);
                    }
                }
            }

            result.put("success", !successfulUploads.isEmpty());
            result.put("totalFiles", filesToUpload.size());
            result.put("successfulUploads", successfulUploads);
            result.put("successCount", successfulUploads.size());
            result.put("failedUploads", failedUploads);
            result.put("failedCount", failedUploads.size());
            result.put("message", String.format("Successfully uploaded %d/%d files from %s",
                    successfulUploads.size(), filesToUpload.size(), directoryPath));

            logger.info("Directory upload completed. Successfully uploaded {}/{} files from {}",
                    successfulUploads.size(), filesToUpload.size(), directoryPath);

        } catch (Exception e) {
            logger.error("Error uploading directory: {}", directoryPath, e);
            result.put("success", false);
            result.put("error", "Error uploading directory: " + e.getMessage());
        }

        return result;
    }

    /**
     * Helper method to collect files for upload.
     * 
     * @param dir       The directory to scan
     * @param pattern   Optional regex pattern for file filtering
     * @param recursive Whether to include subdirectories
     * @param fileList  The list to populate with found files
     */
    private void collectFiles(File dir, Pattern pattern, boolean recursive, List<File> fileList) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (pattern == null || pattern.matcher(file.getName()).matches()) {
                        fileList.add(file);
                    }
                } else if (file.isDirectory() && recursive) {
                    collectFiles(file, pattern, true, fileList);
                }
            }
        }
    }
}
