/*
 * Copyright 2026 the original author or authors.
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

package io.jdev.miniprofiler.jakarta.ee;

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.Timing;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * CDI interceptor that wraps EJB method invocations in a MiniProfiler timing step.
 * Apply {@link Profiled} to a class or method to enable profiling.
 */
@Interceptor
@Profiled
public class ProfilingEJBInterceptor {

    @Inject
    private ProfilerProvider profilerProvider;

    /**
     * Wraps the intercepted method in a MiniProfiler {@link io.jdev.miniprofiler.Timing} step.
     *
     * @param ctx the invocation context
     * @return the result of {@link jakarta.interceptor.InvocationContext#proceed()}
     * @throws Exception if the intercepted method throws
     */
    @AroundInvoke
    public Object profile(InvocationContext ctx) throws Exception {
        Profiler profiler = profilerProvider.current();
        String stepName = String.format("%s.%s", ctx.getTarget().getClass().getSimpleName(), ctx.getMethod().getName());
        Timing timing = profiler.step(stepName);
        try {
            return ctx.proceed();
        } finally {
            timing.close();
        }
    }

}
