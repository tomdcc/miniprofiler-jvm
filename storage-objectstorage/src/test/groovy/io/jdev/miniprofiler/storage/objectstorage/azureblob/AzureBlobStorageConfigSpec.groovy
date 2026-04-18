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

import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfigSpec

class AzureBlobStorageConfigSpec extends BaseObjectStorageConfigSpec {

    String getSystemPropPrefix() { "miniprofiler.storage.azure-blob." }
    String getFilePropPrefix() { "storage.azure-blob." }
    String getBucketPropertyKey() { "container" }

    BaseObjectStorageConfig createConfig(Properties sysprops, Properties fileProps) {
        AzureBlobStorageConfig.create(sysprops, fileProps)
    }

    def "reads all properties with container key"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.azure-blob.container"] = "c"
        sysprops["miniprofiler.storage.azure-blob.prefix"]    = "p/"
        sysprops["miniprofiler.storage.azure-blob.endpoint"]  = "http://localhost:10000"

        when:
        def config = AzureBlobStorageConfig.create(sysprops, null)

        then:
        config.bucketName == "c"
        config.containerName == "c"
        config.prefix == "p/"
        config.region == null
        config.endpoint == "http://localhost:10000"
    }

    def "reads container from system property"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.azure-blob.container"] = "my-container"

        when:
        def config = AzureBlobStorageConfig.create(sysprops, null)

        then:
        config.containerName == "my-container"
        config.configured
    }
}
