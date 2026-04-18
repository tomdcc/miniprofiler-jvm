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

import spock.lang.Specification

class S3StorageConfigSpec extends Specification {

    def "reads bucket from system property"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.s3.bucket"] = "my-bucket"

        when:
        def config = S3StorageConfig.create(sysprops, null)

        then:
        config.bucketName == "my-bucket"
        config.configured
    }

    def "reads bucket from file property"() {
        given:
        def fileProps = new Properties()
        fileProps["storage.s3.bucket"] = "file-bucket"

        when:
        def config = S3StorageConfig.create(new Properties(), fileProps)

        then:
        config.bucketName == "file-bucket"
    }

    def "system property overrides file property"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.s3.bucket"] = "sys-bucket"
        def fileProps = new Properties()
        fileProps["storage.s3.bucket"] = "file-bucket"

        when:
        def config = S3StorageConfig.create(sysprops, fileProps)

        then:
        config.bucketName == "sys-bucket"
    }

    def "reads all properties"() {
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

    def "null bucket means not configured"() {
        when:
        def config = S3StorageConfig.create(new Properties(), null)

        then:
        !config.configured
        config.bucketName == null
    }

    def "empty string bucket value treated as null (not configured)"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.s3.bucket"] = ""

        when:
        def config = S3StorageConfig.create(sysprops, null)

        then:
        !config.configured
    }
}
