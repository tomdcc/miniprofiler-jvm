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

package io.jdev.miniprofiler;

import io.jdev.miniprofiler.internal.NullProfiler;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.Storage;

import java.util.UUID;

/**
 * Primary interface for starting a profiling session and getting a
 * handle on the current session.
 *
 * <p>If your system uses any form of dependency injection, the usual
 * way to do things is to inject a ProfilerProvider instance into
 * anything that need to be profiled, which can then call
 * {@link #getCurrent()} to get a handle on the current
 * provider and add timing steps.</p>
 *
 * <p>If your system doesn't use dependency injection, then code starting
 * a new session should call {@link MiniProfiler#start(String)} and
 * {@link io.jdev.miniprofiler.MiniProfiler#current()},
 * which defers to a static profiler provider instance. That
 * instance can be set using
 * {@link MiniProfiler#setProfilerProvider(ProfilerProvider)}.</p>
 */
public interface ProfilerProvider {

    /**
     * Start a new profiling session with the default {@link ProfileLevel#Info} level.
     *
     * @param rootName the name of the root timing step. This might often be the uri of the current request.
     * @return the new profiler
     */
    Profiler start(String rootName);

    /**
     * Start a new profiling session with the default {@link ProfileLevel#Info} level.
     *
     * @param id       the UUID to use
     * @param rootName the name of the root timing step. This might often be the uri of the current request.
     * @return the new profiler
     */
    Profiler start(UUID id, String rootName);

    /**
     * Start a new profiling session with the given level.
     *
     * @param rootName the name of the root timing step. This might often be the uri of the current request.
     * @param level    the level of the profiling session
     * @return the new profiler
     */
    Profiler start(String rootName, ProfileLevel level);

    /**
     * Start a new profiling session with the given level, root name and UUID.
     *
     * @param id       the UUID to use
     * @param rootName the name of the root timing step. This might often be the uri of the current request.
     * @param level    the level of the profiling session
     * @return the new profiler
     */
    Profiler start(UUID id, String rootName, ProfileLevel level);

    /**
     * Ends the current profiling session, if one exists.
     *
     * <p>Generally it is preferrable to stop a profiling session by
     * calling {@link io.jdev.miniprofiler.Profiler#stop()}, but in some circumstances
     * it may be easier to call this method.</p>
     *
     * @param discardResults When true, clears the miniprofiler for this request, allowing profiling to
     *                       be prematurely stopped and discarded. Useful for when a specific route does not need to be profiled.
     */
    void stopCurrentSession(boolean discardResults);

    /**
     * Marks the given profiling session as stopped. This is generally
     * called from inside the {@link ProfilerImpl#stop()}
     * method. End users do not need to call it. Only public so that
     * custom ProfilerProviders can be developed.
     *
     * @param profilingSession the profiler to register as stopped
     * @param discardResults   When true, clears the miniprofiler for this request, allowing profiling to
     *                         be prematurely stopped and discarded. Useful for when a specific route does not need to be profiled.
     */
    void stopSession(ProfilerImpl profilingSession, boolean discardResults);

    /**
     * Returns the current MiniProfiler.
     *
     * <p>This method should never return null. If there is no current profiling session,
     * a {@link NullProfiler} instance will be returned so that calling code does not
     * have to do null checks around every timing block.</p>
     *
     * @return the current profiler
     */
    Profiler current();

    /**
     * A properties-friendly version of {@link #current()}.
     *
     * @return the current profiler
     */
    Profiler getCurrent();

    /**
     * Return true if there is a current profiler.
     *
     * @return true if there is a current profiler
     */
    boolean hasCurrent();

    /**
     * Returns the {@link Storage} associated with this provider.
     *
     * @return the provider's storage
     */
    Storage getStorage();

    /**
     * Sets the {@link Storage} for this provider to use.
     *
     * @param storage the storage to use
     */
    void setStorage(Storage storage);

    /**
     * Returns the {@link ProfilerUiConfig} associated with this provider.
     *
     * @return the provider's UI config
     */
    ProfilerUiConfig getUiConfig();

    /**
     * Sets the {@link ProfilerUiConfig} for this provider to use.
     *
     * @param uiConfig the UI config to use
     */
    void setUiConfig(ProfilerUiConfig uiConfig);

}
