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

import io.jdev.miniprofiler.internal.ConfigHelper;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static io.jdev.miniprofiler.internal.ConfigHelper.getProperty;
import static io.jdev.miniprofiler.internal.ConfigHelper.loadPropertiesFile;

/**
 * Configuration for the Azure Blob Storage backend.
 *
 * <p>Properties are read from system properties (prefix {@code miniprofiler.storage.azure-blob.})
 * and from {@code miniprofiler.properties} on the classpath (prefix {@code storage.azure-blob.}).
 * System properties take precedence.</p>
 *
 * <p>Supported keys: {@code container}, {@code prefix}, {@code endpoint}.
 * The {@code container} property maps to the base class {@code bucketName} field.</p>
 */
public class AzureBlobStorageConfig extends BaseObjectStorageConfig {

    private static final String SYSTEM_PROP_PREFIX = "miniprofiler.storage.azure-blob.";
    private static final String FILE_PROP_PREFIX = "storage.azure-blob.";
    private static final String MINIPROFILER_RESOURCE_NAME = "/miniprofiler.properties";

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
        return create(System.getProperties(), loadPropertiesFile(MINIPROFILER_RESOURCE_NAME));
    }

    static AzureBlobStorageConfig create(Properties systemProps, Properties fileProps) {
        List<ConfigHelper.PropertiesWithPrefix> propsList = new ArrayList<>(2);
        propsList.add(new ConfigHelper.PropertiesWithPrefix(systemProps, SYSTEM_PROP_PREFIX));
        if (fileProps != null) {
            propsList.add(new ConfigHelper.PropertiesWithPrefix(fileProps, FILE_PROP_PREFIX));
        }
        String container = getProperty(propsList, "container", (String) null);
        String prefix    = getProperty(propsList, "prefix",    (String) null);
        String endpoint  = getProperty(propsList, "endpoint",  (String) null);
        return new AzureBlobStorageConfig(container, prefix, endpoint);
    }
}
