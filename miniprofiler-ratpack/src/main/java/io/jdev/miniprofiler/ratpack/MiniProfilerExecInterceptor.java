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
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.Block;
import ratpack.http.Request;

import java.util.Optional;

/**
 * A Ratpack `ExecInterceptor` that provides MiniProfiler support.
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
public class MiniProfilerExecInterceptor implements ExecInterceptor {

    private final ProfilerProvider provider;
    private final ProfilerStoreOption defaultProfilerStoreOption;

    /**
     * Construct the interceptor with the given provider and default storage option.
     *
     * @param provider the profiler provider to use
     * @param defaultProfilerStoreOption the default profiler storage behaviour
     */
    public MiniProfilerExecInterceptor(ProfilerProvider provider, ProfilerStoreOption defaultProfilerStoreOption) {
        this.provider = provider;
        if (defaultProfilerStoreOption == null) {
           throw new IllegalArgumentException("defaultProfilerStoreOption cannot be null");
        }
        this.defaultProfilerStoreOption = defaultProfilerStoreOption;
    }

    /**
     * Construct the interceptor with the given provider and a default to store all profiler results.
     *
     * @param provider the profiler provider to use
     */
    public MiniProfilerExecInterceptor(ProfilerProvider provider) {
        this(provider, ProfilerStoreOption.STORE_RESULTS);
    }

    /**
     * Intercept the given execution and bind a new Profiler object
     *
     * <p>
     * The {@link ProfilerProvider} that this interceptor was created with will always get bound to the execution.
     * </p>
     * @param execution the execution whose segment is being intercepted
     * @param execType indicates whether this is a compute (e.g. request handling) segment or blocking segment
     * @param continuation the “rest” of the execution
     * @throws Exception any
     */
    @Override
    public void intercept(Execution execution, ExecType execType, Block continuation) throws Exception {
        // create a profiler if there isn't one already
        if (!provider.hasCurrentProfiler()) {
            Profiler profiler = provider.start(getProfilerName(execution, execType));
            execution.onComplete(() -> executionComplete(execution, profiler));
        }
        continuation.execute();
    }

    private void executionComplete(Execution execution, Profiler profiler) {
        ProfilerStoreOption store = execution.maybeGet(ProfilerStoreOption.class).orElse(defaultProfilerStoreOption);
        profiler.stop(store == ProfilerStoreOption.DISCARD_RESULTS);
    }

    /**
     * Override to customize the name given to the profiling instance.
     *
     * <p>Default implementation looks for a Ratpack {@link Request} and uses the URI on that if
     * one is found, otherwise the string <code>"Unknown"</code>.</p>
     *
     * @param execution the execution whose segment is being intercepted
     * @param execType indicates whether this is a compute (e.g. request handling) segment or blocking segment
     * @return the name to be used for the new profiling session
     */
    protected String getProfilerName(Execution execution, ExecType execType) {
        Optional<Request> maybeReq = execution.maybeGet(Request.class);
        return maybeReq.isPresent() ? maybeReq.get().getUri() : "Unknown";
    }
}
