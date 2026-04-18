/*
 * Copyright 2015-2026 the original author or authors.
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

package io.jdev.miniprofiler.test

import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.storage.Storage

/**
 * Storage class for testing.
 */
public class TestStorage implements Storage {

    ProfilerImpl profiler

    @Override
    public Collection<UUID> list(int maxResults, Date start, Date finish, Storage.ListResultsOrder orderBy) {
        throw new UnsupportedOperationException()
    }

    @Override
    public void save(ProfilerImpl profiler) {
        this.profiler = profiler
    }

    @Override
    public ProfilerImpl load(UUID id) {
        profiler.id == id ? profiler : null
    }

    Map<String, Set<UUID>> unviewedByUser = [:]

    @Override
    public void setUnviewed(String user, UUID id) {
        if (user == null) { return }
        unviewedByUser.computeIfAbsent(user, { [] as LinkedHashSet }).add(id)
    }

    @Override
    public void setViewed(String user, UUID id) {
        if (user == null) { return }
        unviewedByUser[user]?.remove(id)
    }

    @Override
    public Collection<UUID> getUnviewedIds(String user) {
        if (user == null) { return [] }
        unviewedByUser[user] ?: []
    }
}
