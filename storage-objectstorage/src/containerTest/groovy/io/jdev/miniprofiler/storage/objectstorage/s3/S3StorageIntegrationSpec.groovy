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

package io.jdev.miniprofiler.storage.objectstorage.s3

import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorage
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageIntegrationSpec
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import spock.lang.Shared

class S3StorageIntegrationSpec extends BaseObjectStorageIntegrationSpec {

    static final String BUCKET = "test-bucket"
    static final int PORT = 9090

    @Shared GenericContainer<?> container
    @Shared S3Client s3Client
    @Shared S3Storage s3Storage

    BaseObjectStorage getStorage() { s3Storage }

    void setupSpec() {
        container = new GenericContainer<>(System.getProperty("dockerImage.s3mock"))
            .withExposedPorts(PORT)
            .waitingFor(Wait.forListeningPort())
        container.start()

        String endpoint = "http://${container.host}:${container.getMappedPort(PORT)}"
        def config = new S3StorageConfig(BUCKET, null, "us-east-1", endpoint)
        s3Client = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .region(Region.of("us-east-1"))
            .endpointOverride(URI.create(endpoint))
            .forcePathStyle(true)
            .build()
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build())
        s3Storage = new S3Storage(config, s3Client)
    }

    void cleanupSpec() {
        s3Storage?.close()
        s3Client?.close()
        container?.stop()
    }
}
