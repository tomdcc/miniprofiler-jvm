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

import io.jdev.miniprofiler.internal.ConfigHelper;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static io.jdev.miniprofiler.internal.ConfigHelper.getProperty;
import static io.jdev.miniprofiler.internal.ConfigHelper.loadPropertiesFile;

/**
 * Configuration for the AWS S3 storage backend.
 *
 * <p>Properties are read from system properties (prefix {@code miniprofiler.storage.s3.})
 * and from {@code miniprofiler.properties} on the classpath (prefix {@code storage.s3.}).
 * System properties take precedence.</p>
 *
 * <p>Supported keys: {@code bucket}, {@code prefix}, {@code region}, {@code endpoint}.</p>
 */
public class S3StorageConfig extends BaseObjectStorageConfig {

    private static final String SYSTEM_PROP_PREFIX = "miniprofiler.storage.s3.";
    private static final String FILE_PROP_PREFIX = "storage.s3.";
    private static final String MINIPROFILER_RESOURCE_NAME = "/miniprofiler.properties";

    /**
     * Creates a new instance with explicit values and default expiry.
     *
     * @param bucket   the S3 bucket name; may be {@code null}
     * @param prefix   the optional key prefix; may be {@code null}
     * @param region   the AWS region; may be {@code null}
     * @param endpoint the endpoint URL override; may be {@code null}
     */
    public S3StorageConfig(String bucket, String prefix, String region, String endpoint) {
        this(bucket, prefix, region, endpoint, DEFAULT_EXPIRY_HOURS);
    }

    /**
     * Creates a new instance with explicit values.
     *
     * @param bucket      the S3 bucket name; may be {@code null}
     * @param prefix      the optional key prefix; may be {@code null}
     * @param region      the AWS region; may be {@code null}
     * @param endpoint    the endpoint URL override; may be {@code null}
     * @param expiryHours hours after which sessions are expired; zero or negative disables
     */
    public S3StorageConfig(String bucket, String prefix, String region, String endpoint, int expiryHours) {
        super(bucket, prefix, region, endpoint, expiryHours);
    }

    /**
     * Creates a new {@link S3StorageConfig} from system properties and
     * {@code miniprofiler.properties}, falling back to {@code null} for each unset property.
     *
     * @return a new config populated from the environment
     */
    public static S3StorageConfig create() {
        return create(System.getProperties(), loadPropertiesFile(MINIPROFILER_RESOURCE_NAME));
    }

    static S3StorageConfig create(Properties systemProps, Properties fileProps) {
        List<ConfigHelper.PropertiesWithPrefix> propsList = new ArrayList<>(2);
        propsList.add(new ConfigHelper.PropertiesWithPrefix(systemProps, SYSTEM_PROP_PREFIX));
        if (fileProps != null) {
            propsList.add(new ConfigHelper.PropertiesWithPrefix(fileProps, FILE_PROP_PREFIX));
        }
        String bucket    = getProperty(propsList, "bucket",      (String) null);
        String prefix    = getProperty(propsList, "prefix",      (String) null);
        String region    = getProperty(propsList, "region",      (String) null);
        String endpoint  = getProperty(propsList, "endpoint",    (String) null);
        int expiryHours  = getProperty(propsList, "expiryHours", DEFAULT_EXPIRY_HOURS);
        return new S3StorageConfig(bucket, prefix, region, endpoint, expiryHours);
    }
}
