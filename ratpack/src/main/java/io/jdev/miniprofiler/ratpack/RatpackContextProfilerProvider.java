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

package io.jdev.miniprofiler.ratpack;

import io.jdev.miniprofiler.BaseProfilerProvider;
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.ExecutionException;
import ratpack.exec.Operation;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link io.jdev.miniprofiler.ProfilerProvider} that keeps profilers that it creates on the current Ratpack
 * {@link Execution} rather than e.g. a {@link ThreadLocal}, since Ratpack executions span across multiple
 * threads.
 */
public class RatpackContextProfilerProvider extends BaseProfilerProvider {

    private final ExecController execController;

    /**
     * Constructs the provider with the {@link ExecController} used to persist profiles.
     *
     * <p>The controller is used to fork a fresh {@link Execution} for the (asynchronous) save when a profiler
     * is stopped outside of a managed Ratpack execution &mdash; for example on a plain thread-pool thread or in
     * an execution's completion handler. A Ratpack application always has an {@code ExecController} available;
     * inject it (e.g. via Guice) and pass it here.</p>
     *
     * @param execController the exec controller to fork a save execution from when needed; must not be null
     */
    public RatpackContextProfilerProvider(ExecController execController) {
        this.execController = Objects.requireNonNull(execController, "execController");
    }

    /**
     * Adds the given rofiler to the current {@link Execution}.
     *
     * @param profiler the newly created profiler
     * @throws ratpack.exec.UnmanagedThreadException if there is no current execution
     */
    @Override
    protected void profilerCreated(Profiler profiler) {
        Execution.current().add(Profiler.class, profiler);
    }

    /**
     * Does nothing.
     *
     * <p> In theory we could remove the profiler from the execution here, but there's no need as executions
     * are throwaway. This method is really here to support cleaning up threadlocals in thread pools.</p>
     * @param profiler the stopped profiler
     */
    @Override
    protected void profilerStopped(Profiler profiler) {
    }

    @Override
    protected void saveProfiler(ProfilerImpl currentProfiler) {
        if (Execution.isManagedThread()) {
            try {
                // On a live execution: bind the save to it, so it is persisted in order with the rest of
                // the execution (e.g. before the request finishes and the UI fetches the result).
                saveOperation(currentProfiler).then();
                return;
            } catch (ExecutionException e) {
                // Managed thread, but the current execution has already completed (e.g. we are being
                // called from its completion handler) and can no longer accept a promise. Fall through
                // to fork a fresh execution below.
            }
        }
        // Either not on a managed thread at all, or the current execution has completed. Fork a fresh
        // execution to run the save, otherwise it would be silently lost.
        execController.fork().start(execution -> saveOperation(currentProfiler).then());
    }

    private Operation saveOperation(ProfilerImpl currentProfiler) {
        String user = currentProfiler.getUser();
        AsyncStorage asyncStorage = AsyncStorage.adapt(getStorage());
        if (user != null) {
            return asyncStorage.saveAsync(currentProfiler)
                .next(asyncStorage.setUnviewedAsync(user, currentProfiler.getId()));
        }
        return asyncStorage.saveAsync(currentProfiler);
    }

    /**
     * Grabs the current profiler from the current execution.
     *
     * @return the current profiler or <code>null</code> if there isn't one or there is no current execution
     */
    @Override
    protected Profiler lookupCurrentProfiler() {
        if(Execution.isManagedThread()) {
            return lookupCurrentProfiler(Execution.current()).orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Returns the current profiler from the given execution, or empty if none is present.
     *
     * @param execution the current Ratpack execution
     * @return the current profiler, or empty if not present
     */
    protected Optional<Profiler> lookupCurrentProfiler(Execution execution) {
        return execution.maybeGet(Profiler.class);
    }

    /**
     * Asynchronously closes the storage associated with this provider.
     *
     * @return an operation that closes the provider's storage
     */
    public Operation closeAsync() {
        return AsyncStorage.adapt(getStorage()).closeAsync();
    }
}
