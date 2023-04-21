/*
 * Copyright 2015 the original author or authors.
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
import ratpack.exec.Execution;

import java.util.Optional;

/**
 * A {@link io.jdev.miniprofiler.ProfilerProvider} that keeps profilers that it creates on the current Ratpack
 * {@link Execution} rather than e.g. a {@link ThreadLocal}, since Ratpack executions span across multiple
 * threads.
 */
public class RatpackContextProfilerProvider extends BaseProfilerProvider {

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
        AsyncStorage.adapt(storage)
            .saveAsync(currentProfiler)
            .then();
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

    protected Optional<Profiler> lookupCurrentProfiler(Execution execution) {
        return execution.maybeGet(Profiler.class);
    }
}
