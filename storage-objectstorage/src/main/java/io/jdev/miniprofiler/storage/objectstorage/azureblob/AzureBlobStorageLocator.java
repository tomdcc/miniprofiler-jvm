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

package io.jdev.miniprofiler.storage.objectstorage.azureblob;

import io.jdev.miniprofiler.storage.Storage;
import io.jdev.miniprofiler.storage.StorageLocator;

import java.util.Optional;

/**
 * {@link StorageLocator} for Azure Blob Storage. Returns an empty {@link Optional} when the
 * {@code miniprofiler.storage.azure-blob.container} property (or file equivalent) is not set,
 * making auto-discovery safe in environments without Azure configuration.
 */
public class AzureBlobStorageLocator implements StorageLocator {

    /** Creates a new instance. */
    public AzureBlobStorageLocator() {
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public Optional<Storage> locate() {
        try {
            AzureBlobStorageConfig config = AzureBlobStorageConfig.create();
            if (!config.isConfigured()) {
                return Optional.empty();
            }
            return Optional.of(new AzureBlobStorage(config));
        } catch (Exception | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }
}
