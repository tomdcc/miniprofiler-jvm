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

package io.jdev.miniprofiler.storage.objectstorage.gcs;

import io.jdev.miniprofiler.internal.ConfigHelper;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static io.jdev.miniprofiler.internal.ConfigHelper.getProperty;
import static io.jdev.miniprofiler.internal.ConfigHelper.loadPropertiesFile;

/**
 * Configuration for the Google Cloud Storage backend.
 *
 * <p>Properties are read from system properties (prefix {@code miniprofiler.storage.gcs.})
 * and from {@code miniprofiler.properties} on the classpath (prefix {@code storage.gcs.}).
 * System properties take precedence.</p>
 *
 * <p>Supported keys: {@code bucket}, {@code prefix}, {@code endpoint}.
 * GCS is a global service so no {@code region} property is used.</p>
 */
public class GcsStorageConfig extends BaseObjectStorageConfig {

    private static final String SYSTEM_PROP_PREFIX = "miniprofiler.storage.gcs.";
    private static final String FILE_PROP_PREFIX = "storage.gcs.";
    private static final String MINIPROFILER_RESOURCE_NAME = "/miniprofiler.properties";

    /**
     * Creates a new instance with explicit values.
     *
     * @param bucket   the GCS bucket name; may be {@code null}
     * @param prefix   the optional key prefix; may be {@code null}
     * @param endpoint the optional host override for the GCS client; may be {@code null}
     */
    public GcsStorageConfig(String bucket, String prefix, String endpoint) {
        super(bucket, prefix, null, endpoint);
    }

    /**
     * Creates a new {@link GcsStorageConfig} from system properties and
     * {@code miniprofiler.properties}, falling back to {@code null} for each unset property.
     *
     * @return a new config populated from the environment
     */
    public static GcsStorageConfig create() {
        return create(System.getProperties(), loadPropertiesFile(MINIPROFILER_RESOURCE_NAME));
    }

    static GcsStorageConfig create(Properties systemProps, Properties fileProps) {
        List<ConfigHelper.PropertiesWithPrefix> propsList = new ArrayList<>(2);
        propsList.add(new ConfigHelper.PropertiesWithPrefix(systemProps, SYSTEM_PROP_PREFIX));
        if (fileProps != null) {
            propsList.add(new ConfigHelper.PropertiesWithPrefix(fileProps, FILE_PROP_PREFIX));
        }
        String bucket   = getProperty(propsList, "bucket",   (String) null);
        String prefix   = getProperty(propsList, "prefix",   (String) null);
        String endpoint = getProperty(propsList, "endpoint", (String) null);
        return new GcsStorageConfig(bucket, prefix, endpoint);
    }
}
