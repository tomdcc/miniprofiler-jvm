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

import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfigSpec

class S3StorageConfigSpec extends BaseObjectStorageConfigSpec {

    String getSystemPropPrefix() { "miniprofiler.storage.s3." }
    String getFilePropPrefix() { "storage.s3." }

    BaseObjectStorageConfig createConfig(Properties sysprops, Properties fileProps) {
        S3StorageConfig.create(sysprops, fileProps)
    }

    def "reads all properties including region"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.s3.bucket"]   = "b"
        sysprops["miniprofiler.storage.s3.prefix"]   = "p/"
        sysprops["miniprofiler.storage.s3.region"]   = "us-east-1"
        sysprops["miniprofiler.storage.s3.endpoint"] = "http://localhost:9090"

        when:
        def config = S3StorageConfig.create(sysprops, null)

        then:
        config.bucketName == "b"
        config.prefix == "p/"
        config.region == "us-east-1"
        config.endpoint == "http://localhost:9090"
    }
}
