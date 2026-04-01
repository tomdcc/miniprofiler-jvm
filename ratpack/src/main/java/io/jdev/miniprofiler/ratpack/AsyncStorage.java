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

package io.jdev.miniprofiler.ratpack;

import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.Storage;
import ratpack.exec.Blocking;
import ratpack.exec.Operation;
import ratpack.exec.Promise;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 * Storage interface for storing profiling session information.
 *
 * <p>Profiling info is stored for later retrieval either by AJAX from the
 * mini-profiler UI on the same page, or later inspection.</p>
 */
public interface AsyncStorage extends Storage {

    /**
     * List profiling
     *
     * @param maxResults the maximum number of ids to list.
     * @param start      the date to start searching
     * @param finish     the end date
     * @param orderBy    which order to list the uuids
     * @return a promise of a list of UUIDs that match the dates
     */
    default Promise<Collection<UUID>> listAsync(int maxResults, Date start, Date finish, ListResultsOrder orderBy) {
        return Blocking.get(() -> list(maxResults, start, finish, orderBy));
    }

    /**
     * Stores the given profiling information
     *
     * @param profiler the profiling information to store
     */
    default Operation saveAsync(ProfilerImpl profiler) {
        return Blocking.op(() -> save(profiler));
    }

    /**
     * Returns a previously saved profiling session.
     *
     * @param id the id of the profiling session to fetch
     * @return the profiler, or null if not found
     */
    default Promise<ProfilerImpl> loadAsync(UUID id) {
        return Blocking.get(() -> load(id));
    }

    /**
     * Sets a particular profiler session so it is considered "un-viewed" by the given user
     *
     * @param user the user
     * @param id   the id of the session
     */
    default Operation setUnviewedAsync(String user, UUID id) {
        return Blocking.op(() -> setUnviewed(user, id));
    }

    /**
     * Sets a particular profiler session to "viewed" for the given user.
     *
     * @param user the user
     * @param id   the id of the session
     */
    default Operation setViewedAsync(String user, UUID id) {
        return Blocking.op(() -> setViewed(user, id));
    }

    /**
     * Returns a list of profiling session ids s that haven't been seen by the given user.
     *
     * @param user the user
     * @return a list of ids that the user hasn't viewed
     */
    default Promise<Collection<UUID>> getUnviewedIdsAsync(String user) {
        return Blocking.get(() -> getUnviewedIds(user));
    }

    static AsyncStorage adapt(Storage storage) {
        if (storage instanceof AsyncStorage) {
            return (AsyncStorage) storage;
        } else {
            return new AsyncStorage() {
                @Override
                public Collection<UUID> list(int maxResults, Date start, Date finish, ListResultsOrder orderBy) {
                    return storage.list(maxResults, start, finish, orderBy);
                }

                @Override
                public void save(ProfilerImpl profiler) {
                    storage.save(profiler);
                }

                @Override
                public ProfilerImpl load(UUID id) {
                    return storage.load(id);
                }

                @Override
                public void setUnviewed(String user, UUID id) {
                    storage.setUnviewed(user, id);
                }

                @Override
                public void setViewed(String user, UUID id) {
                    storage.setViewed(user, id);
                }

                @Override
                public Collection<UUID> getUnviewedIds(String user) {
                    return storage.getUnviewedIds(user);
                }
            };
        }
    }
}
