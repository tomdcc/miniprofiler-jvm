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

package io.jdev.miniprofiler.storage.objectstorage.azureblob

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorage
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageIntegrationSpec
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Shared

class AzureBlobStorageIntegrationSpec extends BaseObjectStorageIntegrationSpec {

    static final String CONTAINER = "test-container"
    static final int PORT = 10000
    static final String ACCOUNT_NAME = "devstoreaccount1"
    static final String ACCOUNT_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="

    @Shared GenericContainer<?> container
    @Shared BlobServiceClient blobServiceClient
    @Shared AzureBlobStorage azureStorage

    BaseObjectStorage getStorage() { azureStorage }

    void setupSpec() {
        container = new GenericContainer<>("mcr.microsoft.com/azure-storage/azurite:latest")
            .withExposedPorts(PORT)
            .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--loose", "--skipApiVersionCheck")
            .waitingFor(Wait.forListeningPort())
        container.start()

        String endpoint = "http://${container.host}:${container.getMappedPort(PORT)}/${ACCOUNT_NAME}"
        String connectionString = "DefaultEndpointsProtocol=http;" +
            "AccountName=${ACCOUNT_NAME};" +
            "AccountKey=${ACCOUNT_KEY};" +
            "BlobEndpoint=${endpoint};"

        blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()
        blobServiceClient.createBlobContainer(CONTAINER)

        def config = new AzureBlobStorageConfig(CONTAINER, null, null)
        azureStorage = new AzureBlobStorage(config, blobServiceClient)
    }

    void cleanupSpec() {
        azureStorage?.close()
        container?.stop()
    }
}
