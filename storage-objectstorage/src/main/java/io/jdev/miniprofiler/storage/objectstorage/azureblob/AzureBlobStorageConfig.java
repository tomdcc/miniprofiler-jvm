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

import io.jdev.miniprofiler.MiniProfilerConfig;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig;

import java.util.Properties;

/**
 * Configuration for the Azure Blob Storage backend.
 *
 * <p>Properties are read via {@link io.jdev.miniprofiler.MiniProfilerConfig}: system properties
 * (prefix {@code miniprofiler.}) take precedence over {@code miniprofiler.properties} on the classpath.</p>
 *
 * <p>Supported keys: {@code storage.azure-blob.container}, {@code storage.azure-blob.prefix},
 * {@code storage.azure-blob.endpoint}.
 * The {@code container} property maps to the base class {@code bucketName} field.</p>
 */
public class AzureBlobStorageConfig extends BaseObjectStorageConfig {

    /**
     * Creates a new instance with explicit values.
     *
     * @param container the Azure Blob container name; may be {@code null}
     * @param prefix    the optional key prefix; may be {@code null}
     * @param endpoint  the Azure Blob endpoint URL; may be {@code null}
     */
    public AzureBlobStorageConfig(String container, String prefix, String endpoint) {
        super(container, prefix, null, endpoint);
    }

    /**
     * Convenience accessor that returns the container name.
     *
     * @return the container name (delegates to {@link #getBucketName()})
     */
    public String getContainerName() {
        return getBucketName();
    }

    /**
     * Creates a new {@link AzureBlobStorageConfig} from system properties and
     * {@code miniprofiler.properties}, falling back to {@code null} for each unset property.
     *
     * @return a new config populated from the environment
     */
    public static AzureBlobStorageConfig create() {
        return create(new MiniProfilerConfig());
    }

    static AzureBlobStorageConfig create(Properties systemProps, Properties fileProps) {
        return create(new MiniProfilerConfig(systemProps, fileProps));
    }

    static AzureBlobStorageConfig create(MiniProfilerConfig props) {
        String container = props.getProperty("storage.azure-blob.container", (String) null);
        String prefix    = props.getProperty("storage.azure-blob.prefix",    (String) null);
        String endpoint  = props.getProperty("storage.azure-blob.endpoint",  (String) null);
        return new AzureBlobStorageConfig(container, prefix, endpoint);
    }
}
