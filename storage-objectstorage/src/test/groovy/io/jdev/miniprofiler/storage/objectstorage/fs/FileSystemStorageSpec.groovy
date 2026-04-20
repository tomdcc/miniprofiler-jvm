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

package io.jdev.miniprofiler.storage.objectstorage.fs

import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorage
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageIntegrationSpec
import spock.lang.Shared
import spock.lang.TempDir

import java.nio.file.Path

class FileSystemStorageSpec extends BaseObjectStorageIntegrationSpec {

    @TempDir
    @Shared
    Path tempDir

    @Shared FileSystemStorage fsStorage

    BaseObjectStorage getStorage() { fsStorage }

    void setupSpec() {
        def config = new FileSystemStorageConfig(tempDir, null)
        fsStorage = new FileSystemStorage(config)
    }

    void cleanupSpec() {
        fsStorage?.close()
    }
}
