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

import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.BaseStorage;
import io.jdev.miniprofiler.storage.Storage;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for object-storage-backed {@link Storage} implementations.
 *
 * <p>Implements all {@link Storage} methods in terms of five abstract protected
 * operations ({@link #putObject}, {@link #getObject}, {@link #deleteObject},
 * {@link #listKeys}, {@link #closeClient}) that subclasses must provide.</p>
 *
 * <p>Three key namespaces are used (see {@link ObjectStorageKeys}):</p>
 * <ul>
 *   <li>{@code profiler/{uuid}} — serialised profiler JSON</li>
 *   <li>{@code profiler-index/{timestamp}-{uuid}} — empty listing marker</li>
 *   <li>{@code unviewed/{user}/{uuid}} — empty unviewed marker</li>
 * </ul>
 */
public abstract class BaseObjectStorage extends BaseStorage {

    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final Duration DEFAULT_SCHEDULE_INTERVAL = Duration.ofHours(1);

    /** Key calculator for this storage instance. */
    protected final ObjectStorageKeys keys;
    /** The bucket or container name. */
    protected final String bucket;
    private final boolean ownsClient;
    private final ScheduledExecutorService scheduler;
    private final Duration expiryAge;
    private volatile boolean closed;

    /**
     * Creates a new instance. If the config specifies a positive {@code expiryHours},
     * a background scheduler is started that runs an hourly cleanup job.
     *
     * @param config     the storage configuration
     * @param ownsClient {@code true} if this instance owns the cloud client and should
     *                   close it when {@link #close()} is called
     */
    protected BaseObjectStorage(BaseObjectStorageConfig config, boolean ownsClient) {
        this(config, ownsClient,
            config.getExpiryHours() > 0 ? Duration.ofHours(config.getExpiryHours()) : null,
            DEFAULT_SCHEDULE_INTERVAL);
    }

    /**
     * Creates a new instance with explicit expiry age and schedule interval. Intended
     * for testing with short durations.
     *
     * @param config           the storage configuration
     * @param ownsClient       {@code true} if this instance owns the cloud client
     * @param expiryAge        maximum age of profiling sessions; {@code null} or non-positive
     *                         disables automatic expiry
     * @param scheduleInterval interval between expiry runs; must not be {@code null} when
     *                         {@code expiryAge} is positive
     */
    protected BaseObjectStorage(BaseObjectStorageConfig config, boolean ownsClient,
                                Duration expiryAge, Duration scheduleInterval) {
        this.keys = new ObjectStorageKeys(config.getPrefix());
        this.bucket = config.getBucketName();
        this.ownsClient = ownsClient;
        if (expiryAge != null && !expiryAge.isNegative() && !expiryAge.isZero()) {
            this.expiryAge = expiryAge;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "miniprofiler-storage-expiry");
                t.setDaemon(true);
                return t;
            });
            long intervalMs = scheduleInterval.toMillis();
            this.scheduler.scheduleAtFixedRate(this::runExpiry, intervalMs, intervalMs,
                TimeUnit.MILLISECONDS);
        } else {
            this.expiryAge = null;
            this.scheduler = null;
        }
    }

    /**
     * Stores the given bytes at the specified key, creating or overwriting the object.
     *
     * @param key     the object key
     * @param content the bytes to store
     */
    protected abstract void putObject(String key, byte[] content);

    /**
     * Returns the bytes stored at the specified key, or {@code null} if the object
     * does not exist.
     *
     * @param key the object key
     * @return the stored bytes, or {@code null} if not found
     */
    protected abstract byte[] getObject(String key);

    /**
     * Deletes the object at the specified key. No-op if the object does not exist.
     *
     * @param key the object key
     */
    protected abstract void deleteObject(String key);

    /**
     * Lists all object keys whose names begin with the given prefix.
     *
     * @param keyPrefix the key prefix to filter by
     * @return the matching keys
     */
    protected abstract Collection<String> listKeys(String keyPrefix);

    /**
     * Closes the underlying cloud client. Called by {@link #close()} only when
     * this instance owns the client.
     */
    protected abstract void closeClient();

    @Override
    public void save(ProfilerImpl profiler) {
        byte[] data = profiler.toJSONString().getBytes(StandardCharsets.UTF_8);
        putObject(keys.profilerKey(profiler.getId()), data);
        putObject(keys.indexKey(profiler.getStarted(), profiler.getId()), EMPTY_BYTES);
    }

    @Override
    public ProfilerImpl load(UUID id) {
        byte[] data = getObject(keys.profilerKey(id));
        if (data == null) {
            return null;
        }
        return ProfilerImpl.fromJson(new String(data, StandardCharsets.UTF_8));
    }

    @Override
    public Collection<UUID> list(int maxResults, Date start, Date finish, ListResultsOrder orderBy) {
        long startMs = start != null ? start.getTime() : 0L;
        long finishMs = finish != null ? finish.getTime() : Long.MAX_VALUE;

        List<String> indexKeys = new ArrayList<String>(listKeys(keys.indexPrefix()));
        Collections.sort(indexKeys);
        if (orderBy == ListResultsOrder.Descending) {
            Collections.reverse(indexKeys);
        }

        String idxPfx = keys.indexPrefix();
        List<UUID> result = new ArrayList<UUID>();
        for (String key : indexKeys) {
            if (result.size() >= maxResults) {
                break;
            }
            // extract timestamp from key: after prefix, first 19 chars
            String afterPrefix = key.substring(idxPfx.length());
            if (afterPrefix.length() < 19) {
                continue;
            }
            long timestamp;
            try {
                timestamp = Long.parseLong(afterPrefix.substring(0, 19));
            } catch (NumberFormatException e) {
                continue;
            }
            if (timestamp < startMs || timestamp > finishMs) {
                continue;
            }
            UUID id = keys.extractIdFromIndexKey(key);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    @Override
    public void setUnviewed(String user, UUID id) {
        if (user == null || id == null) {
            return;
        }
        putObject(keys.unviewedKey(user, id), EMPTY_BYTES);
    }

    @Override
    public void setViewed(String user, UUID id) {
        if (user == null || id == null) {
            return;
        }
        deleteObject(keys.unviewedKey(user, id));
    }

    @Override
    public Collection<UUID> getUnviewedIds(String user) {
        if (user == null) {
            return Collections.emptyList();
        }
        Collection<String> unviewedKeys = listKeys(keys.unviewedPrefix(user));
        String unviewedPfx = keys.unviewedPrefix(user);
        List<UUID> result = new ArrayList<UUID>();
        for (String key : unviewedKeys) {
            String tail = key.substring(unviewedPfx.length());
            try {
                result.add(UUID.fromString(tail));
            } catch (IllegalArgumentException e) {
                // malformed key — skip
            }
        }
        return result;
    }

    /** {@inheritDoc} Idempotent. Shuts down the expiry scheduler and closes the underlying client if this instance owns it. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (ownsClient) {
            closeClient();
        }
    }

    private void runExpiry() {
        expireOlderThan(Instant.now().minus(expiryAge));
    }

    @Override
    public void clear() {
        for (String key : listKeys(keys.profilerPrefix())) {
            deleteObject(key);
        }
        for (String key : listKeys(keys.indexPrefix())) {
            deleteObject(key);
        }
        for (String key : listKeys(keys.allUnviewedPrefix())) {
            deleteObject(key);
        }
    }

    @Override
    public void expireOlderThan(Instant cutoff) {
        long cutoffMs = cutoff.toEpochMilli();
        String idxPfx = keys.indexPrefix();
        for (String key : listKeys(idxPfx)) {
            String afterPrefix = key.substring(idxPfx.length());
            if (afterPrefix.length() < 19) {
                continue;
            }
            long timestamp;
            try {
                timestamp = Long.parseLong(afterPrefix.substring(0, 19));
            } catch (NumberFormatException e) {
                continue;
            }
            if (timestamp < cutoffMs) {
                UUID id = keys.extractIdFromIndexKey(key);
                deleteObject(key);
                if (id != null) {
                    deleteObject(keys.profilerKey(id));
                }
            }
        }
    }
}
