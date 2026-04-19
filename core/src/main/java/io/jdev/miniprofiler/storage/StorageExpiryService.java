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

package io.jdev.miniprofiler.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules periodic calls to {@link Storage#expireOlderThan} on a background daemon thread.
 *
 * <p>Storage implementations only need to implement {@link Storage#expireOlderThan}; this class
 * handles the scheduling and lifecycle of the background cleanup task. Instances are created
 * and closed by the owning {@link io.jdev.miniprofiler.ProfilerProvider}.</p>
 */
public class StorageExpiryService implements AutoCloseable {

    /** Default interval between expiry runs: one hour. */
    public static final Duration DEFAULT_SCHEDULE_INTERVAL = Duration.ofHours(1);

    private final ScheduledExecutorService scheduler;
    private volatile boolean closed;

    /**
     * Creates a new service with an explicit schedule interval.
     *
     * @param storage          the storage to expire entries from
     * @param expiryAge        sessions older than this duration are removed on each run
     * @param scheduleInterval how often to run the expiry task
     */
    public StorageExpiryService(Storage storage, Duration expiryAge, Duration scheduleInterval) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "miniprofiler-storage-expiry");
            t.setDaemon(true);
            return t;
        });
        long intervalMs = scheduleInterval.toMillis();
        this.scheduler.scheduleAtFixedRate(
            () -> storage.expireOlderThan(Instant.now().minus(expiryAge)),
            intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Shuts down the background scheduler. Idempotent: subsequent calls have no effect.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            scheduler.shutdownNow();
        }
    }
}
