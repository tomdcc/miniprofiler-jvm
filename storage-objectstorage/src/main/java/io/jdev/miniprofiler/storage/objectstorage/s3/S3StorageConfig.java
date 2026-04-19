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

package io.jdev.miniprofiler.storage.objectstorage.s3;

import io.jdev.miniprofiler.MiniProfilerConfig;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig;

import java.util.Properties;

/**
 * Configuration for the AWS S3 storage backend.
 *
 * <p>Properties are read via {@link io.jdev.miniprofiler.MiniProfilerConfig}: system properties
 * (prefix {@code miniprofiler.}) take precedence over {@code miniprofiler.properties} on the classpath.</p>
 *
 * <p>Supported keys: {@code storage.s3.bucket}, {@code storage.s3.prefix},
 * {@code storage.s3.region}, {@code storage.s3.endpoint}.</p>
 */
public class S3StorageConfig extends BaseObjectStorageConfig {

    /**
     * Creates a new instance with explicit values.
     *
     * @param bucket   the S3 bucket name; may be {@code null}
     * @param prefix   the optional key prefix; may be {@code null}
     * @param region   the AWS region; may be {@code null}
     * @param endpoint the endpoint URL override; may be {@code null}
     */
    public S3StorageConfig(String bucket, String prefix, String region, String endpoint) {
        super(bucket, prefix, region, endpoint);
    }

    /**
     * Creates a new {@link S3StorageConfig} from system properties and
     * {@code miniprofiler.properties}, falling back to {@code null} for each unset property.
     *
     * @return a new config populated from the environment
     */
    public static S3StorageConfig create() {
        return create(new MiniProfilerConfig());
    }

    static S3StorageConfig create(Properties systemProps, Properties fileProps) {
        return create(new MiniProfilerConfig(systemProps, fileProps));
    }

    static S3StorageConfig create(MiniProfilerConfig props) {
        String bucket   = props.getProperty("storage.s3.bucket",   (String) null);
        String prefix   = props.getProperty("storage.s3.prefix",   (String) null);
        String region   = props.getProperty("storage.s3.region",   (String) null);
        String endpoint = props.getProperty("storage.s3.endpoint", (String) null);
        return new S3StorageConfig(bucket, prefix, region, endpoint);
    }
}
