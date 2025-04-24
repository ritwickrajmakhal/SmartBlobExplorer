package io.github.ritwickrajmakhal;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class BlobClient {
    private BlobServiceClient blobServiceClient;
    private BlobContainerClient blobContainerClient;

    public BlobClient(String connectionString, String containerName) {

        blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    public BlobServiceClient getBlobServiceClient() {
        return blobServiceClient;
    }

    // List all blobs with metadata ✅
    public List<String> listBlobs() {
        List<String> blobDetails = new ArrayList<>();
        blobContainerClient.listBlobs().forEach(blobItem -> {
            blobDetails.add(String.format("Name: %s, Size: %d, Last Modified: %s",
                    blobItem.getName(),
                    blobItem.getProperties().getContentLength(),
                    blobItem.getProperties().getLastModified()));
        });
        return blobDetails;
    }

    // Upload a file ✅
    public boolean uploadFile(String filePath) {
        try {
            File file;
            String blobName;

            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                // Download the file from the URL
                URL url = new URL(filePath);
                blobName = new File(url.getPath()).getName(); // Extract the original file name from the URL
                file = File.createTempFile("temp-", ".tmp");
                try (InputStream in = url.openStream()) {
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
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

            // Clean up temporary file if created
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                file.delete();
            }

            return true;
        } catch (Exception e) {
            System.err.println("❌ Error uploading file: " + e.getMessage());
            return false;
        }
    }

    // Download a blob ✅
    public void downloadBlob(String blobName, String destinationPath) {
        try {
            File destinationFile = new File(destinationPath);
            if (destinationFile.isDirectory()) {
                // Append the blob name to the directory path
                destinationPath = new File(destinationPath, blobName).getAbsolutePath();
            }
            BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(blobName).getBlockBlobClient();
            blockBlobClient.downloadToFile(destinationPath, true);
        } catch (Exception e) {
            System.err.println("❌ Error downloading blob: " + e.getMessage());
        }
    }

    // Delete a blob ✅
    public boolean deleteBlob(String blobName) {
        try {
            blobContainerClient.getBlobClient(blobName).delete();
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error deleting blob: " + e.getMessage());
            return false;
        }
    }

    // Rename or move a blob ✅
    public boolean renameBlob(String sourceBlobName, String destinationBlobName) {
        BlockBlobClient sourceBlobClient = blobContainerClient.getBlobClient(sourceBlobName).getBlockBlobClient();
        BlockBlobClient destinationBlobClient = blobContainerClient.getBlobClient(destinationBlobName)
                .getBlockBlobClient();
        try {
            destinationBlobClient.beginCopy(sourceBlobClient.getBlobUrl(), null);
            sourceBlobClient.delete();
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error renaming blob: " + e.getMessage());
            return false;
        }
    }
}
