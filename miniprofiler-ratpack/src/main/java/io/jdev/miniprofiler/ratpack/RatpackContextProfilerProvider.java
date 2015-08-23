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
import ratpack.exec.Execution;

public class RatpackContextProfilerProvider extends BaseProfilerProvider {

    @Override
    protected void profilerCreated(Profiler profiler) {
        Execution.current().add(Profiler.class, profiler);
    }

    @Override
    protected void profilerStopped(Profiler profiler) {
        // In theory we could remove the profiler here using Execution.current().remove(Profiler.class),
        // but there's no need as executions are throwaway. This method is really here to support
        // cleaning up threadlocals in thread pools.
    }

    @Override
    protected Profiler lookupCurrentProfiler() {
        return Execution.current().maybeGet(Profiler.class).orElse(null);
    }
}
