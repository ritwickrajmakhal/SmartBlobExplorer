package io.github.ritwickrajmakhal;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Client for interacting with Azure Blob Storage.
 * <p>
 * This class provides methods for common blob operations including:
 * <ul>
 * <li>Uploading files to blob storage from local paths or URLs</li>
 * <li>Downloading blobs to the local file system</li>
 * <li>Deleting blobs</li>
 * <li>Renaming/moving blobs within a container</li>
 * </ul>
 * <p>
 * The client connects to a specific Azure Storage account and container
 * using a connection string provided during initialization.
 */
public class BlobClient {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(BlobClient.class);

    /** Client for interacting with the Azure Blob Storage service */
    private BlobServiceClient blobServiceClient;

    /** Client for interacting with a specific blob container */
    private BlobContainerClient blobContainerClient;

    /**
     * Creates a new BlobClient that connects to a specific container in Azure Blob
     * Storage.
     * 
     * @param connectionString The Azure Storage account connection string
     * @param containerName    The name of the blob container to connect to
     */
    public BlobClient(String connectionString, String containerName) {
        logger.debug("Initializing BlobClient with container: {}", containerName);

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
                logger.debug("File is a URL, downloading to temporary location");
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
            logger.debug("Creating blob client for: {}", blobName);
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
     */
    public void downloadBlob(String blobName, String destinationPath) {
        try {
            logger.info("Downloading blob: {} to path: {}", blobName, destinationPath);

            File destinationFile = new File(destinationPath);
            if (destinationFile.isDirectory()) {
                // Append the blob name to the directory path
                destinationPath = new File(destinationPath, blobName).getAbsolutePath();
                logger.debug("Destination is a directory, full path will be: {}", destinationPath);
            }

            BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(blobName).getBlockBlobClient();
            // Use downloadToFile instead of manually handling streams
            blockBlobClient.downloadToFile(destinationPath, true);

            logger.info("Successfully downloaded blob: {} to: {}", blobName, destinationPath);
        } catch (Exception e) {
            logger.error("Error downloading blob: {}", blobName, e);
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
     * source blob
     * at the destination path and then deleting the original. If the operation
     * fails during
     * copying, any partially created destination blob will be cleaned up.
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
            logger.debug("Starting copy operation from source to destination");
            destinationBlobClient.beginCopy(sourceBlobClient.getBlobUrl(), null);

            // Wait for the copy operation to complete (optional but ensures consistency)
            // For large files, you might want to implement a polling mechanism instead
            logger.debug("Waiting for copy operation to complete");
            Thread.sleep(500);

            // Now delete the source blob
            logger.debug("Deleting source blob after successful copy");
            sourceBlobClient.delete();

            logger.info("Successfully renamed blob from: {} to: {}", sourceBlobName, destinationBlobName);
            return true;
        } catch (Exception e) {
            logger.error("Error renaming blob from: {} to: {}", sourceBlobName, destinationBlobName, e);
            // If a destination blob was created but the operation didn't complete fully
            // we should clean it up in the error case
            try {
                if (destinationBlobClient.exists()) {
                    logger.debug("Cleaning up partially created destination blob");
                    destinationBlobClient.delete();
                }
            } catch (Exception ex) {
                logger.error("Error cleaning up partial rename for destination: {}", destinationBlobName, ex);
            }
            return false;
        }
    }
}
