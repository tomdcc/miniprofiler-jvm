/*
 * Copyright 2013 the original author or authors.
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

import io.jdev.miniprofiler.ProfilerImpl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Storage implementation based on an LRU in-memory map.
 */
public class MapStorage extends BaseStorage {
    private static final int DEFAULT_MAX_SIZE = 500;

    private final Map<UUID, ProfilerImpl> cache;

    public MapStorage() {
        this(DEFAULT_MAX_SIZE);
    }

    public MapStorage(int maxSize) {
        cache = Collections.synchronizedMap(new LRUMapCache(maxSize));
    }

    public void save(ProfilerImpl profiler) {
        cache.put(profiler.getId(), profiler);
    }

    public ProfilerImpl load(UUID id) {
        return cache.get(id);
    }

    private static class LRUMapCache extends LinkedHashMap<UUID, ProfilerImpl> {
        private int maxSize;

        LRUMapCache(int maxSize) {
            super(10, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, ProfilerImpl> eldest) {
            return size() > maxSize;
        }
    }
}
