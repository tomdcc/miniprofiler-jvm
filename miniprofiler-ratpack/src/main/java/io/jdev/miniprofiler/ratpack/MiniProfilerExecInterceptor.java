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

import io.jdev.miniprofiler.MiniProfiler;
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.Block;
import ratpack.http.Request;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Function;

/**
 * A Ratpack `ExecInterceptor` that provides MiniProfiler support.
 *
 * <p>
 * It binds the current {@link ProfilerProvider} to the execution being intercepted, and starts
 * a profiling session and binds that to the execution as well, unless {@link #shouldProfile}
 * is overridden to return false.
 * </p>
 */
public class MiniProfilerExecInterceptor implements ExecInterceptor {

    private final ProfilerProvider profilerProvider;

    /**
     * Basic convenience factory method which creates a Ratpack execution-based profiler provider and binds it
     * to the static profiler provider instance as per {@link MiniProfiler#setProfilerProvider}.
     *
     * @return an interceptor populated with a {@link RatpackContextProfilerProvider}
     */
    public static MiniProfilerExecInterceptor create() {
        ProfilerProvider profilerProvider = new RatpackContextProfilerProvider();
        MiniProfiler.setProfilerProvider(profilerProvider);
        return new MiniProfilerExecInterceptor(profilerProvider);
    }

    /**
     * Creates an interceptor with the provided {@link ProfilerProvider} instance.
     *
     * @param profilerProvider the {@link ProfilerProvider} to use.
     */
    @Inject
    public MiniProfilerExecInterceptor(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
    }

    /**
     * Intercept the given execution and bind MiniProfiler related objects.
     *
     * <p>
     * The {@link ProfilerProvider} that this interceptor was created with will always get bound to the execution.
     * </p>
     * <p>
     * If {@link #shouldProfile} returns true (the default), a new profiler will be created
     * and bound to the execution.
     * </p>
     * @param execution the execution whose segment is being intercepted
     * @param execType indicates whether this is a compute (e.g. request handling) segment or blocking segment
     * @param continuation the “rest” of the execution
     * @throws Exception any
     */
    @Override
    public void intercept(Execution execution, ExecType execType, Block continuation) throws Exception {
        execution.add(ProfilerProvider.class, profilerProvider);
        if (shouldProfile(execution, execType)) {
            Optional<Profiler> existingProfiler = execution.maybeGet(Profiler.class);
            if (!existingProfiler.isPresent()) {
                final Profiler profiler = profilerProvider.start(getProfilerName(execution, execType));
                execution.onCleanup(profiler);
            }
        }
        continuation.execute();
    }

    /**
     * Override to switch off profiling for certain executions. Default implementation always returns true.
     * @param execution the execution whose segment is being intercepted
     * @param execType indicates whether this is a compute (e.g. request handling) segment or blocking segment
     * @return true if the execution should be profiled. Default is always true.
     */
    protected boolean shouldProfile(Execution execution, ExecType execType) {
        return true;
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
