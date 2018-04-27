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

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import ratpack.exec.ExecInitializer;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.http.Request;
import ratpack.http.Response;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.jdev.miniprofiler.ratpack.internal.MiniProfilerRatpackUtilInternal.getUUIDRequestId;

/**
 * A Ratpack `ExecInitializer` that provides MiniProfiler support.
 *
 * <p>It starts a profiling session and binds that to the execution.</p>
 *
 * <p>
 *     If the <code>defaultProfilerStoreOption</code> constructor argument is
 *     {@link ProfilerStoreOption#STORE_RESULTS}, the profiler will be saved to storage
 *     at the end of the execution. if that value is set to {@link ProfilerStoreOption#DISCARD_RESULTS},
 *     the profiler results will not be saved. The default can be overridden by
 *     binding one of those values to the execution during execution, or by
 *     adding one of {@link StoreMiniProfilerHandler} or {@link DiscardMiniProfilerHandler}
 *     to the handler chain.
 * </p>
 */
public class MiniProfilerExecInitializer implements ExecInitializer {

    private final ProfilerProvider provider;
    private final ProfilerStoreOption defaultProfilerStoreOption;

    /**
     * Construct the initializer with the given provider and default storage option.
     *
     * @param provider the profiler provider to use
     * @param defaultProfilerStoreOption the default profiler storage behaviour
     */
    public MiniProfilerExecInitializer(ProfilerProvider provider, ProfilerStoreOption defaultProfilerStoreOption) {
        this.provider = provider;
        if (defaultProfilerStoreOption == null) {
           throw new IllegalArgumentException("defaultProfilerStoreOption cannot be null");
        }
        this.defaultProfilerStoreOption = defaultProfilerStoreOption;
    }

    /**
     * Construct the initializer with the given provider and a default to store all profiler results.
     *
     * @param provider the profiler provider to use
     */
    public MiniProfilerExecInitializer(ProfilerProvider provider) {
        this(provider, ProfilerStoreOption.STORE_RESULTS);
    }

    /**
     * Initialize the given execution and bind a new Profiler object
     *
     * <p>
     * The {@link ProfilerProvider} that this initializer was created with will always get bound to the execution.
     * </p>
     * @param execution the execution whose segment is being intercepted
     */
    @Override
    public void init(Execution execution) {
        // create a profiler if there isn't one already
        if (shouldCreateProfilerOnExecutionStart(execution) && !provider.hasCurrent()) {
            provider.start(getUUIDRequestId(execution), getProfilerName(execution));
        }
        Completion completion = new Completion(execution);
        execution.onComplete(completion);
        // try to complete the profiler before the response is sent, if there is one
        execution.maybeGet(Response.class).ifPresent(r -> r.beforeSend(completion));
    }

    protected void executionComplete(Execution execution) {
        if (provider.hasCurrent()) {
            Profiler profiler = provider.current();
            ProfilerStoreOption store = execution.maybeGet(ProfilerStoreOption.class).orElse(defaultProfilerStoreOption);
            profiler.stop(store == ProfilerStoreOption.DISCARD_RESULTS);
        }
    }

    /**
     * Override to customize the name given to the profiling instance.
     *
     * <p>Default implementation looks for a Ratpack {@link Request} and uses the URI on that if
     * one is found, otherwise the string <code>"Unknown"</code>.</p>
     *
     * @param execution the execution whose segment is being intercepted
     * @return the name to be used for the new profiling session
     */
    protected String getProfilerName(Execution execution) {
        Optional<Request> maybeReq = execution.maybeGet(Request.class);
        return maybeReq.isPresent() ? maybeReq.get().getUri() : "Unknown";
    }

    /**
     * Controls whether this initializer will start a profiler at the start of the execution for the given execution.
     *
     * <p>Default is to return false, ie profilers are started by some other execution logic, rather than for all
     * executions.</p>
     *
     * @param execution the execution whose segment is being initialized
     * @return <code>false</code> for the default implementation
     */
    protected boolean shouldCreateProfilerOnExecutionStart(Execution execution) {
        return false;
    }


    private class Completion implements AutoCloseable, Action<Response> {
        private final Execution execution;
        private final AtomicBoolean completed = new AtomicBoolean(false);

        private Completion(Execution execution) {
            this.execution = execution;
        }

        private void complete() {
            if(!completed.getAndSet(true)) {
                executionComplete(execution);
            }
        }

        @Override
        public void close() throws Exception {
            complete();
        }

        @Override
        public void execute(Response response) throws Exception {
            complete();
        }
    }
}
