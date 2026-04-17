/*
 * Copyright 2013-2026 the original author or authors.
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

import io.jdev.miniprofiler.internal.ProfilerImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Storage implementation based on an LRU in-memory map.
 */
public class MapStorage extends BaseStorage {
    private static final int DEFAULT_MAX_SIZE = 500;

    private final Map<UUID, ProfilerImpl> cache;
    private final ConcurrentHashMap<String, Set<UUID>> unviewedByUser = new ConcurrentHashMap<>();

    /** Creates a new instance with the default maximum size of {@value DEFAULT_MAX_SIZE} entries. */
    public MapStorage() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Creates a new instance with the given maximum number of cached profiler sessions.
     *
     * @param maxSize the maximum number of profiler sessions to retain
     */
    public MapStorage(int maxSize) {
        cache = Collections.synchronizedMap(new LRUMapCache(maxSize));
    }

    @Override
    public void save(ProfilerImpl profiler) {
        cache.put(profiler.getId(), profiler);
    }

    @Override
    public ProfilerImpl load(UUID id) {
        return cache.get(id);
    }

    @Override
    public Collection<UUID> list(int maxResults, Date start, Date finish, ListResultsOrder orderBy) {
        long startMs = start != null ? start.getTime() : 0;
        long finishMs = finish != null ? finish.getTime() : Long.MAX_VALUE;
        List<ProfilerImpl> filtered;
        synchronized (cache) {
            filtered = new ArrayList<>(cache.values()).stream()
                .filter(p -> p.getStarted() >= startMs && p.getStarted() <= finishMs)
                .collect(Collectors.toList());
        }
        Comparator<ProfilerImpl> cmp = Comparator.comparingLong(ProfilerImpl::getStarted);
        if (orderBy == ListResultsOrder.Descending) {
            cmp = cmp.reversed();
        }
        return filtered.stream()
            .sorted(cmp)
            .limit(maxResults)
            .map(ProfilerImpl::getId)
            .collect(Collectors.toList());
    }

    @Override
    public void setUnviewed(String user, UUID id) {
        if (user == null || id == null) {
            return;
        }
        unviewedByUser.computeIfAbsent(user, k -> ConcurrentHashMap.newKeySet()).add(id);
    }

    @Override
    public void setViewed(String user, UUID id) {
        if (user == null || id == null) {
            return;
        }
        Set<UUID> set = unviewedByUser.get(user);
        if (set != null) {
            set.remove(id);
        }
    }

    @Override
    public Collection<UUID> getUnviewedIds(String user) {
        if (user == null) {
            return Collections.emptyList();
        }
        Set<UUID> set = unviewedByUser.get(user);
        if (set == null) {
            return Collections.emptyList();
        }
        List<UUID> result = new ArrayList<>();
        Iterator<UUID> it = set.iterator();
        while (it.hasNext()) {
            UUID uid = it.next();
            if (cache.containsKey(uid)) {
                result.add(uid);
            } else {
                it.remove();
            }
        }
        return result;
    }

    /** Removes all cached profiler sessions and resets unviewed state. */
    public void clear() {
        cache.clear();
        unviewedByUser.clear();
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
