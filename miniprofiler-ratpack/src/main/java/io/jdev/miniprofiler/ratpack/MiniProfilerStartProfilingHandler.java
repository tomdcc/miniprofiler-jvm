/*
 * Copyright 2016 the original author or authors.
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

import io.jdev.miniprofiler.ProfilerProvider;
import ratpack.handling.Context;
import ratpack.handling.Handler;

/**
 * A handler that starts a new profiler for the current execution.
 *
 * <p>The {@link #shouldStartProfiler(Context)} method can be overridden to optionally start a profiler. By default,
 * the handler will always start one.</p>
 */
public class MiniProfilerStartProfilingHandler implements Handler {

    private final ProfilerProvider profilerProvider;

    public MiniProfilerStartProfilingHandler(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        if (shouldStartProfiler(ctx)) {
            profilerProvider.start(ctx.getRequest().getUri());
        }
        ctx.next();
    }

    /**
     * Indicates whether the handler should start a profiler for the current request.
     *
     * @param ctx the current Ratpack context
     * @return Default implementation always returns <code>true</code>.
     */
    protected boolean shouldStartProfiler(Context ctx) {
        return true;
    }
}
