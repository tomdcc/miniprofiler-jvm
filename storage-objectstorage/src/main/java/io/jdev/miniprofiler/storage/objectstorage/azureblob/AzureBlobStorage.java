/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdev.miniprofiler.storage.objectstorage.azureblob;

import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Azure Blob Storage-backed {@link io.jdev.miniprofiler.storage.Storage} implementation.
 *
 * <p>The Azure SDK ({@code com.azure:azure-storage-blob} and {@code com.azure:azure-identity})
 * is a {@code compileOnly} dependency — users must provide it on their own classpath.</p>
 */
public class AzureBlobStorage extends BaseObjectStorage {

    private static final int HTTP_NOT_FOUND = 404;

    private final BlobServiceClient blobServiceClient;

    /**
     * Creates storage using the given config and token credential; the client is owned by this
     * instance. Note that {@link BlobServiceClient} does not implement {@code Closeable},
     * so {@link #closeClient()} is a no-op.
     *
     * @param config     the Azure Blob storage configuration
     * @param credential the Azure token credential to use
     */
    public AzureBlobStorage(AzureBlobStorageConfig config, TokenCredential credential) {
        super(config, true);
        this.blobServiceClient = buildClient(config, credential);
    }

    /**
     * Creates storage using the given config and default Azure credentials; the client is
     * owned by this instance.
     *
     * @param config the Azure Blob storage configuration
     */
    public AzureBlobStorage(AzureBlobStorageConfig config) {
        this(config, new DefaultAzureCredentialBuilder().build());
    }

    /**
     * Creates storage using a provided {@link BlobServiceClient}; the client will not be
     * closed on {@link #close()} (and {@link BlobServiceClient} does not implement
     * {@code Closeable} in any case).
     *
     * @param config            the Azure Blob storage configuration
     * @param blobServiceClient the Azure Blob service client to use
     */
    public AzureBlobStorage(AzureBlobStorageConfig config, BlobServiceClient blobServiceClient) {
        super(config, false);
        this.blobServiceClient = blobServiceClient;
    }

    /**
     * Creates storage using a provided {@link BlobServiceClient} and configuration read from
     * the environment; the client will not be closed on {@link #close()}.
     *
     * @param blobServiceClient the Azure Blob service client to use
     */
    public AzureBlobStorage(BlobServiceClient blobServiceClient) {
        this(AzureBlobStorageConfig.create(), blobServiceClient);
    }

    /**
     * Creates storage using configuration and credentials read from the environment; the
     * client is owned by this instance.
     */
    public AzureBlobStorage() {
        this(AzureBlobStorageConfig.create());
    }

    private static BlobServiceClient buildClient(AzureBlobStorageConfig config, TokenCredential credential) {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder().credential(credential);
        if (config.getEndpoint() != null) {
            builder.endpoint(config.getEndpoint());
        }
        return builder.buildClient();
    }

    @Override
    protected void putObject(String key, byte[] content) {
        getBlobClient(key).upload(BinaryData.fromBytes(content), true);
    }

    @Override
    protected byte[] getObject(String key) {
        try {
            return getBlobClient(key).downloadContent().toBytes();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == HTTP_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    @Override
    protected void deleteObject(String key) {
        try {
            getBlobClient(key).delete();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() != HTTP_NOT_FOUND) {
                throw e;
            }
            // already deleted — ignore
        }
    }

    @Override
    protected Collection<String> listKeys(String keyPrefix) {
        List<String> result = new ArrayList<String>();
        for (BlobItem item : blobServiceClient.getBlobContainerClient(bucket)
            .listBlobs(new ListBlobsOptions().setPrefix(keyPrefix), null)) {
            result.add(item.getName());
        }
        return result;
    }

    /**
     * No-op — {@link BlobServiceClient} does not implement {@code Closeable}.
     */
    @Override
    protected void closeClient() {
        // BlobServiceClient does not implement Closeable
    }

    private BlobClient getBlobClient(String key) {
        return blobServiceClient.getBlobContainerClient(bucket).getBlobClient(key);
    }
}
