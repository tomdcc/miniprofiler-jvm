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

package io.jdev.miniprofiler.storage.objectstorage.gcs

import com.google.cloud.NoCredentials
import com.google.cloud.storage.BucketInfo
import com.google.cloud.storage.StorageOptions
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorage
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageIntegrationSpec
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Shared

class GcsStorageIntegrationSpec extends BaseObjectStorageIntegrationSpec {

    static final String BUCKET = "test-bucket"
    static final int PORT = 4443

    @Shared GenericContainer<?> container
    @Shared com.google.cloud.storage.Storage gcsClient
    @Shared GcsStorage gcsStorage

    BaseObjectStorage getStorage() { gcsStorage }

    void setupSpec() {
        container = new GenericContainer<>(System.getProperty("dockerImage.fake-gcs-server"))
            .withExposedPorts(PORT)
            .withCommand("-scheme", "http")
            .waitingFor(Wait.forListeningPort())
        container.start()

        String endpoint = "http://${container.host}:${container.getMappedPort(PORT)}"
        gcsClient = StorageOptions.newBuilder()
            .setHost(endpoint)
            .setProjectId("test-project")
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService()
        gcsClient.create(BucketInfo.of(BUCKET))

        def config = new GcsStorageConfig(BUCKET, null, endpoint)
        gcsStorage = new GcsStorage(config, gcsClient)
    }

    void cleanupSpec() {
        gcsStorage?.close()
        container?.stop()
    }
}
