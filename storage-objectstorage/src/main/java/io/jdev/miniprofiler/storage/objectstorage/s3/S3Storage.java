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

package io.jdev.miniprofiler.storage.objectstorage.s3;

import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorage;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * AWS S3-backed {@link io.jdev.miniprofiler.storage.Storage} implementation.
 *
 * <p>The AWS SDK ({@code software.amazon.awssdk:s3} and {@code software.amazon.awssdk:auth})
 * is a {@code compileOnly} dependency — users must provide it on their own classpath.</p>
 */
public class S3Storage extends BaseObjectStorage {

    private final S3Client s3Client;

    /**
     * Creates storage using the given config and credentials provider; the client is owned
     * by this instance and will be closed when {@link #close()} is called.
     *
     * @param config      the S3 storage configuration
     * @param credentials the AWS credentials provider
     */
    public S3Storage(S3StorageConfig config, AwsCredentialsProvider credentials) {
        super(config, true);
        this.s3Client = buildClient(config, credentials);
    }

    /**
     * Creates storage using the given config and default environment credentials; the client
     * is owned by this instance and will be closed when {@link #close()} is called.
     *
     * @param config the S3 storage configuration
     */
    public S3Storage(S3StorageConfig config) {
        this(config, DefaultCredentialsProvider.builder().build());
    }

    /**
     * Creates storage using a provided S3 client; the client will not be closed on
     * {@link #close()}.
     *
     * @param config   the S3 storage configuration
     * @param s3Client the S3 client to use
     */
    public S3Storage(S3StorageConfig config, S3Client s3Client) {
        super(config, false);
        this.s3Client = s3Client;
    }

    /**
     * Creates storage using a provided S3 client and configuration read from the environment;
     * the client will not be closed on {@link #close()}.
     *
     * @param s3Client the S3 client to use
     */
    public S3Storage(S3Client s3Client) {
        this(S3StorageConfig.create(), s3Client);
    }

    /**
     * Creates storage using configuration and credentials read from the environment; the
     * client is owned by this instance and will be closed when {@link #close()} is called.
     */
    public S3Storage() {
        this(S3StorageConfig.create());
    }

    private static S3Client buildClient(S3StorageConfig config, AwsCredentialsProvider credentials) {
        S3ClientBuilder builder = S3Client.builder().credentialsProvider(credentials);
        if (config.getRegion() != null) {
            builder.region(Region.of(config.getRegion()));
        }
        if (config.getEndpoint() != null) {
            builder.endpointOverride(URI.create(config.getEndpoint()));
        }
        return builder.build();
    }

    @Override
    protected void putObject(String key, byte[] content) {
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).build(),
            RequestBody.fromBytes(content)
        );
    }

    @Override
    protected byte[] getObject(String key) {
        try {
            return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build()
            ).asByteArray();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    @Override
    protected void deleteObject(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    protected Collection<String> listKeys(String keyPrefix) {
        ListObjectsV2Iterable pages = s3Client.listObjectsV2Paginator(
            ListObjectsV2Request.builder().bucket(bucket).prefix(keyPrefix).build()
        );
        List<String> result = new ArrayList<String>();
        for (S3Object obj : pages.contents()) {
            result.add(obj.key());
        }
        return result;
    }

    @Override
    protected void closeClient() {
        s3Client.close();
    }
}
