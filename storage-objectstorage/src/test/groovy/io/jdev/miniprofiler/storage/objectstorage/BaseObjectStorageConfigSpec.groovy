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

package io.jdev.miniprofiler.storage.objectstorage

import spock.lang.Specification

abstract class BaseObjectStorageConfigSpec extends Specification {

    abstract String getSystemPropPrefix()

    abstract String getFilePropPrefix()

    abstract BaseObjectStorageConfig createConfig(Properties sysprops, Properties fileProps)

    /** The property key used for the bucket/container name. Defaults to {@code "bucket"}. */
    String getBucketPropertyKey() { "bucket" }

    def "reads bucket from system property"() {
        given:
        def sysprops = new Properties()
        sysprops["${systemPropPrefix}${bucketPropertyKey}"] = "my-bucket"

        when:
        def config = createConfig(sysprops, null)

        then:
        config.bucketName == "my-bucket"
        config.configured
    }

    def "reads bucket from file property"() {
        given:
        def fileProps = new Properties()
        fileProps["${filePropPrefix}${bucketPropertyKey}"] = "file-bucket"

        when:
        def config = createConfig(new Properties(), fileProps)

        then:
        config.bucketName == "file-bucket"
    }

    def "system property overrides file property"() {
        given:
        def sysprops = new Properties()
        sysprops["${systemPropPrefix}${bucketPropertyKey}"] = "sys-bucket"
        def fileProps = new Properties()
        fileProps["${filePropPrefix}${bucketPropertyKey}"] = "file-bucket"

        when:
        def config = createConfig(sysprops, fileProps)

        then:
        config.bucketName == "sys-bucket"
    }

    def "null bucket means not configured"() {
        when:
        def config = createConfig(new Properties(), null)

        then:
        !config.configured
        config.bucketName == null
    }

    def "empty string bucket value treated as null (not configured)"() {
        given:
        def sysprops = new Properties()
        sysprops["${systemPropPrefix}${bucketPropertyKey}"] = ""

        when:
        def config = createConfig(sysprops, null)

        then:
        !config.configured
    }

}
