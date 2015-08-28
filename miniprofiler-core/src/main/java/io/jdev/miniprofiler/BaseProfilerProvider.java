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

import io.jdev.miniprofiler.storage.MapStorage;
import io.jdev.miniprofiler.storage.Storage;
import io.jdev.miniprofiler.user.UserProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Support class for profiler providers. This provides most functionality
 * that a profiler provider will need, except for saving and retrieving
 * the current profiler.
 */
public abstract class BaseProfilerProvider implements ProfilerProvider {
    private Storage storage = new MapStorage();
    private String machineName = getDefaultHostname();
    private UserProvider userProvider;

    /**
     * Called after a new profiler is created. Subclasses should
     * store the passed-in profiler for later retrieval in calls
     * to {@link #getCurrentProfiler()}.
     *
     * @param profiler the newly created profiler
     */
    protected abstract void profilerCreated(Profiler profiler);

    /**
     * Called after a profiler has been stopped. Subsequent calls
     * to {@link #getCurrentProfiler()} should return null.
     *
     * @param profiler the stopped profiler
     */
    protected abstract void profilerStopped(Profiler profiler);


    /**
     * Return the current profiler, if any.
     *
     * @return The current profiler, or null if none.
     */
    protected abstract Profiler lookupCurrentProfiler();

    /**
     * Returns the current MiniProfiler.
     */
    @Override
    public final Profiler getCurrentProfiler() {
        Profiler p = lookupCurrentProfiler();
        return p != null ? p : NullProfiler.INSTANCE;
    }

    /**
     * Indicates whether there is currently a profiler in the context.
     * @return true if there is a profiler currently running
     */
    @Override
    public boolean hasCurrentProfiler() {
        return lookupCurrentProfiler() != null;
    }

    /**
     * Create a new profiling session with Info profiling level.
     *
     * @param rootName A name for the session. Thiis is used as the name
     *                 of the root timing node for the session, and could be
     *                 the currently rendering URL or background job name.
     * @return the newly created profiler
     */
    @Override
    public Profiler start(String rootName) {
        return start(rootName, ProfileLevel.Info);
    }

    /**
     * Create a new profiling session with Info profiling level.
     *
     * @param id       the UUID to use
     * @param rootName A name for the session. Thiis is used as the name
     *                 of the root timing node for the session, and could be
     *                 the currently rendering URL or background job name.
     * @return the newly created profiler
     */
    @Override
    public Profiler start(UUID id, String rootName) {
        return start(id, rootName, ProfileLevel.Info);
    }

    /**
     * Create a new profiling session.
     *
     * @param rootName A name for the session. Thiis is used as the name
     *                 of the root timing node for the session, and could be
     *                 the currently rendering URL or background job name.
     * @param level    The level of detail to use when profiling
     * @return the newly created profiler
     */
    @Override
    public Profiler start(String rootName, ProfileLevel level) {
        return start(null, rootName, level);
    }

    /**
     * Start a new profiling session with the given level, root name and UUID.
     *
     * @param id       the UUID to use
     * @param rootName the name of the root timing step. This might often be the uri of the current request.
     * @param level    the level of the profiling session
     * @return the new profiler
     */
    @Override
    public Profiler start(UUID id, String rootName, ProfileLevel level) {
        ProfilerImpl profiler = new ProfilerImpl(id, rootName, rootName, level, this);
        profiler.setMachineName(machineName);
        if (userProvider != null) {
            profiler.setUser(userProvider.getUser());
        }
        profilerCreated(profiler);
        return profiler;
    }

    /**
     * Ends the current profiling session, if one exists.
     *
     * @param discardResults When true, clears the miniprofiler for this request, allowing profiling to
     *                       be prematurely stopped and discarded. Useful for when a specific route does not need to be profiled.
     */
    @Override
    public void stopCurrentSession(boolean discardResults) {
        Profiler currentProfiler = getCurrentProfiler();
        if (currentProfiler != null) {
            currentProfiler.stop(discardResults);
        }
    }

    /**
     * Marks the given profiling session as stopped.
     *
     * @param profilingSession the profiler to register as stopped
     * @param discardResults   When true, clears the miniprofiler for this request, allowing profiling to
     *                         be prematurely stopped and discarded. Useful for when a specific route does not need to be profiled.
     */
    @Override
    public void stopSession(ProfilerImpl profilingSession, boolean discardResults) {
        profilingSession.stop();
        String currentUser = userProvider != null ? userProvider.getUser() : null;
        if (currentUser != null && profilingSession.getUser() == null) {
            // user wasn't available at profile start, but is now
            profilingSession.setUser(currentUser);
        }
        profilerStopped(profilingSession);
        if (!discardResults) {
            saveProfiler(profilingSession);
        }
    }

    private void saveProfiler(ProfilerImpl currentProfiler) {
        storage.save(currentProfiler);
    }

    /**
     * Set the profiler storage to be used by this provider.
     * <p>The profiler storage is where profiler sessions are stored
     * to be retrieved later, either by an AJAX call immediately
     * after a page render, or for later debugging.</p>
     * <p> By default, the storage property is set to an in-memory
     * {@link MapStorage}.</p>
     *
     * @param storage the storage option to use
     * @see Storage
     */
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
     * Return the current storage implementation.
     *
     * @return current storage
     */
    public Storage getStorage() {
        return storage;
    }

    /**
     * Sets the machine name for the current machine. In unset this defaults
     * to the local host name, as determined by {@link #getDefaultHostname()}.
     *
     * @param machineName the machine name to use
     */
    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    /**
     * Returnes the current machine name set
     *
     * @return the current machine name
     */
    public String getMachineName() {
        return machineName;
    }

    /**
     * Sets the user provider for this profiler provider.
     *
     * @param userProvider The user provider to use.
     * @see UserProvider
     */
    public void setUserProvider(UserProvider userProvider) {
        this.userProvider = userProvider;
    }

    /**
     * Return the local host name, or the loopback name (usually "localhost")
     * if it doesn't resolve in DNS.
     *
     * @return host name as returned by {@link InetAddress#getHostName()}
     */
    public String getDefaultHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // local host name doesn't resolve, just use localhost
            return "localhost";
        }

    }
}
