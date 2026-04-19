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

import io.jdev.miniprofiler.MiniProfilerConfig;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig;

import java.util.Properties;

/**
 * Configuration for the Google Cloud Storage backend.
 *
 * <p>Properties are read via {@link io.jdev.miniprofiler.MiniProfilerConfig}: system properties
 * (prefix {@code miniprofiler.}) take precedence over {@code miniprofiler.properties} on the classpath.</p>
 *
 * <p>Supported keys: {@code storage.gcs.bucket}, {@code storage.gcs.prefix},
 * {@code storage.gcs.endpoint}.
 * GCS is a global service so no {@code region} property is used.</p>
 */
public class GcsStorageConfig extends BaseObjectStorageConfig {

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
        return create(new MiniProfilerConfig());
    }

    static GcsStorageConfig create(Properties systemProps, Properties fileProps) {
        return create(new MiniProfilerConfig(systemProps, fileProps));
    }

    static GcsStorageConfig create(MiniProfilerConfig props) {
        String bucket   = props.getProperty("storage.gcs.bucket",   (String) null);
        String prefix   = props.getProperty("storage.gcs.prefix",   (String) null);
        String endpoint = props.getProperty("storage.gcs.endpoint", (String) null);
        return new GcsStorageConfig(bucket, prefix, endpoint);
    }
}
