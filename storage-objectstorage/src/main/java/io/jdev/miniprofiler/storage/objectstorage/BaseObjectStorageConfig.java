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

package io.jdev.miniprofiler.storage.objectstorage;

/**
 * Base configuration for object storage backends.
 *
 * <p>Holds the bucket/container name, optional key prefix, region, and endpoint
 * override. Subclasses add provider-specific factory methods and property keys.</p>
 */
public abstract class BaseObjectStorageConfig {

    /** Default expiry age in hours. */
    public static final int DEFAULT_EXPIRY_HOURS = 24;

    private final String bucketName;
    private final String prefix;
    private final String region;
    private final String endpoint;
    private final int expiryHours;

    /**
     * Creates a new instance.
     *
     * @param bucketName  the name of the bucket or container; may be {@code null}
     * @param prefix      optional key prefix prepended to all object keys; may be {@code null}
     * @param region      optional cloud region; may be {@code null}
     * @param endpoint    optional endpoint URL override; may be {@code null}
     * @param expiryHours hours after which profiling sessions are automatically expired;
     *                    zero or negative disables automatic expiry
     */
    protected BaseObjectStorageConfig(String bucketName, String prefix, String region,
                                      String endpoint, int expiryHours) {
        this.bucketName = bucketName;
        this.prefix = prefix;
        this.region = region;
        this.endpoint = endpoint;
        this.expiryHours = expiryHours;
    }

    /**
     * Returns the bucket or container name.
     *
     * @return the bucket name, or {@code null} if not configured
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Returns the key prefix.
     *
     * @return the key prefix, or {@code null} if none
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the cloud region.
     *
     * @return the region, or {@code null} if not set
     */
    public String getRegion() {
        return region;
    }

    /**
     * Returns the endpoint URL override.
     *
     * @return the endpoint, or {@code null} if not set
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Returns the expiry age in hours. Profiling sessions older than this are automatically
     * removed. Zero or negative means automatic expiry is disabled.
     *
     * @return the expiry age in hours
     */
    public int getExpiryHours() {
        return expiryHours;
    }

    /**
     * Returns {@code true} if this configuration has a non-empty bucket name.
     *
     * @return {@code true} if configured
     */
    public boolean isConfigured() {
        return bucketName != null && !bucketName.isEmpty();
    }
}
