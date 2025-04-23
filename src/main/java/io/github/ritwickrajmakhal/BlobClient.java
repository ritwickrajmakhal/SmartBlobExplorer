package io.github.ritwickrajmakhal;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import io.github.cdimascio.dotenv.Dotenv;

public class BlobClient {
    private final static Dotenv dotenv = Dotenv.load();
    private BlobServiceClient blobServiceClient;

    public BlobClient(String connectionString) {

        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    public BlobServiceClient getBlobServiceClient() {
        return blobServiceClient;
    }

}
