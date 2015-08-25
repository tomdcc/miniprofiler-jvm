/*
 * Copyright 2014 the original author or authors.
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

package io.jdev.miniprofiler.javaee;

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.Timing;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@Profiled
public class ProfilingEJBInterceptor {

    @Inject
    private ProfilerProvider profilerProvider;

    @AroundInvoke
    public Object profile(InvocationContext ctx) throws Exception {
        Profiler profiler = profilerProvider.getCurrentProfiler();
        String stepName = String.format("%s.%s", ctx.getTarget().getClass().getSimpleName(), ctx.getMethod().getName());
        Timing timing = profiler.step(stepName);
        try {
            return ctx.proceed();
        } finally {
            timing.close();
        }
    }

}
