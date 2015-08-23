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

/**
 * A Ratpack `ExecInterceptor` that creates a mini
 */
public class MiniProfilerExecInterceptor implements ExecInterceptor {

    private final ProfilerProvider profilerProvider;

    /**
     * Basic convenience factory method which creates a ratpack execution-based profiler provider and binds it
     * to the static profiler provider instance as per {@link MiniProfiler#setProfilerProvider}.
     */
    public static MiniProfilerExecInterceptor create() {
        ProfilerProvider profilerProvider = new RatpackContextProfilerProvider();
        MiniProfiler.setProfilerProvider(profilerProvider);
        return new MiniProfilerExecInterceptor(profilerProvider);
    }

    /**
     * Construct the interceptor with an already provided profiler provider.
     */
    @Inject
    public MiniProfilerExecInterceptor(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
    }

    @Override
    public void intercept(Execution execution, ExecType execType, Block continuation) throws Exception {
        if (shouldProfile(execution, execType)) {
            execution.add(ProfilerProvider.class, profilerProvider);
            Optional<Profiler> existingProfiler = execution.maybeGet(Profiler.class);
            if (!existingProfiler.isPresent()) {
                Optional<Request> maybeReq = execution.maybeGet(Request.class);
                String profileName = maybeReq.isPresent() ? maybeReq.get().getUri() : "Unknown";
                final Profiler profiler = profilerProvider.start(profileName);
                execution.add(Profiler.class, profiler);
                execution.onCleanup(profiler);
            }
        }
        continuation.execute();
    }

    /**
     * Override to allow
     * @param execution
     * @param execType
     * @return
     */
    protected boolean shouldProfile(Execution execution, ExecType execType) {
        return true;
    }

    public ProfilerProvider getProfilerProvider() {
        return this.profilerProvider;
    }
}
