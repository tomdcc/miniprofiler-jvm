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

package io.jdev.miniprofiler.viewer;

import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.Storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

/** A read-only {@link Storage} backed by a single MiniProfiler JSON file on disk. */
public class MiniProfilerViewerSingleFileStorage implements Storage {

    private final UUID uuid;
    private final ProfilerImpl profiler;

    private MiniProfilerViewerSingleFileStorage(UUID uuid, ProfilerImpl profiler) {
        this.uuid = uuid;
        this.profiler = profiler;
    }

    /**
     * Creates a storage instance by reading and deserializing the profile JSON at the given path.
     *
     * @param path the path to the MiniProfiler JSON file
     * @return a new storage instance backed by the file
     */
    public static MiniProfilerViewerSingleFileStorage forFile(Path path) {
        if (!path.toFile().isFile()) {
            throw new IllegalArgumentException("File not found: " + path);
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String json = new String(bytes, StandardCharsets.UTF_8);
            ProfilerImpl profiler = ProfilerImpl.fromJson(json);
            return new MiniProfilerViewerSingleFileStorage(profiler.getId(), profiler);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read profile file: " + path, e);
        }
    }

    /**
     * Returns the UUID of the single profiling session stored in this instance.
     *
     * @return the UUID of the stored profiling session
     */
    public UUID getUuid() {
        return uuid;
    }

    /** {@inheritDoc} Returns the single stored session if its start time falls within the given range. */
    @Override
    public Collection<UUID> list(int maxResults, Date start, Date finish, ListResultsOrder orderBy) {
        long started = profiler.getStarted();
        if (started >= start.getTime() && started <= finish.getTime()) {
            return Collections.singletonList(uuid);
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} No-op: this storage is read-only. */
    @Override
    public void save(ProfilerImpl profiler) {
    }

    /** {@inheritDoc} Returns the stored profiler if the given id matches, otherwise null. */
    @Override
    public ProfilerImpl load(UUID id) {
        return uuid.equals(id) ? profiler : null;
    }

    /** {@inheritDoc} No-op. */
    @Override
    public void setUnviewed(String user, UUID id) {
    }

    /** {@inheritDoc} No-op. */
    @Override
    public void setViewed(String user, UUID id) {
    }

    /** {@inheritDoc} Always returns an empty collection. */
    @Override
    public Collection<UUID> getUnviewedIds(String user) {
        return Collections.emptyList();
    }
}
