/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdev.miniprofiler.integtest;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.MapStorage;

/**
 * Represents an in-process server running a MiniProfiler handler implementation.
 * Implementations start the server on construction and stop it on {@link #close()}.
 *
 * <p>Each implementation owns its profiler provider, using the production type appropriate
 * for its environment (e.g. {@link io.jdev.miniprofiler.DefaultProfilerProvider} for servlet
 * and core implementations, {@code RatpackContextProfilerProvider} for Ratpack).
 * Use {@link #getProfilerProvider()} to access it for injecting test profiles.</p>
 */
public interface InProcessTestedServer extends TestedServer {

    /**
     * Returns the profiler provider used by this server.
     * Cast to {@link io.jdev.miniprofiler.BaseProfilerProvider} to access storage.
     *
     * @return the profiler provider
     */
    ProfilerProvider getProfilerProvider();

    /**
     * Returns the URL path (relative to serverUrl) of a page that serves an HTML response
     * with the MiniProfiler floating button widget, profiled with a child step and SQL timing.
     * Returns {@code null} for implementations that don't serve application pages (e.g. the viewer).
     *
     * @return the URL path, or {@code null} if not applicable
     */
    default String getProfiledPagePath() {
        return null;
    }

    /**
     * Returns the URL path (relative to serverUrl) of an endpoint that is profiled and returns
     * the {@code X-MiniProfiler-Ids} response header, suitable for testing AJAX profiling.
     * Returns {@code null} for implementations that don't profile application requests
     * (e.g. core server, viewer).
     *
     * @return the URL path, or {@code null} if not applicable
     */
    default String getAjaxEndpointPath() {
        return null;
    }

    /**
     * Returns a fixed username configured on the server's {@code UserProvider}, or {@code null}
     * if this server does not configure a user provider.
     *
     * <p>Integration tests gated on a non-null value can use this user to verify
     * viewed/unviewed state.</p>
     *
     * @return the test user name, or {@code null}
     */
    default String getTestUser() {
        return null;
    }

    default void addProfile(ProfilerImpl profiler) {
        getProfilerProvider().getStorage().save(profiler);
    }

    default void clearProfiles() {
        ((MapStorage) getProfilerProvider().getStorage()).clear();
    }
}
