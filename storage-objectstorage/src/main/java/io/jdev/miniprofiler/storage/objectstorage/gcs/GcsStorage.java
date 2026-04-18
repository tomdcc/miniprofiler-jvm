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

package io.jdev.miniprofiler.storage.objectstorage.gcs;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.StorageOptions;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Google Cloud Storage-backed {@link io.jdev.miniprofiler.storage.Storage} implementation.
 *
 * <p>The GCS SDK ({@code com.google.cloud:google-cloud-storage} and
 * {@code com.google.auth:google-auth-library-oauth2-http}) is a {@code compileOnly}
 * dependency — users must provide it on their own classpath.</p>
 *
 * <p>Note: {@code com.google.cloud.storage.Storage} conflicts with the miniprofiler
 * {@code Storage} interface name; the GCS client field is referenced by its fully
 * qualified class name throughout this class.</p>
 */
public class GcsStorage extends BaseObjectStorage {

    private final com.google.cloud.storage.Storage gcsClient;

    /**
     * Creates storage using the given config and credentials; the client is owned by this
     * instance and will be closed when {@link #close()} is called.
     *
     * @param config      the GCS storage configuration
     * @param credentials the Google credentials to use
     */
    public GcsStorage(GcsStorageConfig config, Credentials credentials) {
        super(config, true);
        this.gcsClient = buildClient(config, credentials);
    }

    /**
     * Creates storage using the given config and application default credentials; the client
     * is owned by this instance and will be closed when {@link #close()} is called.
     *
     * @param config the GCS storage configuration
     * @throws IOException if the application default credentials cannot be loaded
     */
    public GcsStorage(GcsStorageConfig config) throws IOException {
        this(config, GoogleCredentials.getApplicationDefault());
    }

    /**
     * Creates storage using a provided GCS client; the client will not be closed on
     * {@link #close()}.
     *
     * @param config    the GCS storage configuration
     * @param gcsClient the GCS client to use
     */
    public GcsStorage(GcsStorageConfig config, com.google.cloud.storage.Storage gcsClient) {
        super(config, false);
        this.gcsClient = gcsClient;
    }

    /**
     * Creates storage using a provided GCS client and configuration read from the environment;
     * the client will not be closed on {@link #close()}.
     *
     * @param gcsClient the GCS client to use
     */
    public GcsStorage(com.google.cloud.storage.Storage gcsClient) {
        this(GcsStorageConfig.create(), gcsClient);
    }

    /**
     * Creates storage using configuration and application default credentials read from the
     * environment; the client is owned by this instance and will be closed when
     * {@link #close()} is called.
     *
     * @throws IOException if the application default credentials cannot be loaded
     */
    public GcsStorage() throws IOException {
        this(GcsStorageConfig.create());
    }

    private static com.google.cloud.storage.Storage buildClient(GcsStorageConfig config, Credentials credentials) {
        StorageOptions.Builder builder = StorageOptions.newBuilder().setCredentials(credentials);
        if (config.getEndpoint() != null) {
            builder.setHost(config.getEndpoint());
        }
        return builder.build().getService();
    }

    @Override
    protected void putObject(String key, byte[] content) {
        gcsClient.create(BlobInfo.newBuilder(BlobId.of(bucket, key)).build(), content);
    }

    @Override
    protected byte[] getObject(String key) {
        com.google.cloud.storage.Blob blob = gcsClient.get(BlobId.of(bucket, key));
        if (blob == null || !blob.exists()) {
            return null;
        }
        return blob.getContent();
    }

    @Override
    protected void deleteObject(String key) {
        gcsClient.delete(BlobId.of(bucket, key));
    }

    @Override
    protected Collection<String> listKeys(String keyPrefix) {
        List<String> result = new ArrayList<>();
        for (com.google.cloud.storage.Blob blob : gcsClient.list(bucket,
            com.google.cloud.storage.Storage.BlobListOption.prefix(keyPrefix)).iterateAll()) {
            result.add(blob.getName());
        }
        return result;
    }

    @Override
    protected void closeClient() {
        try {
            gcsClient.close();
        } catch (Exception e) {
            // best-effort close
        }
    }
}
