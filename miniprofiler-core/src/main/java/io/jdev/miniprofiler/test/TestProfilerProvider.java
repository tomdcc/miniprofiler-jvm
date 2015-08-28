/*
 * Copyright 2013 the original author or authors.
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

package io.jdev.miniprofiler.test;

import io.jdev.miniprofiler.BaseProfilerProvider;
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerImpl;

/**
 * Profiler provider to assist testing. Has a single profiler instance and performs
 * no storage.
 */

public class TestProfilerProvider extends BaseProfilerProvider {

    private Profiler profiler;
    private boolean discarded;

    @Override
    protected void profilerCreated(Profiler profiler) {
        this.profiler = profiler;
    }

    @Override
    protected void profilerStopped(Profiler profiler) {
    }

    @Override
    protected Profiler lookupCurrentProfiler() {
        return profiler;
    }

    @Override
    public void stopSession(ProfilerImpl profilingSession, boolean discardResults) {
        discarded = discardResults;
        super.stopSession(profilingSession, discardResults);
    }

    public boolean wasDiscarded() {
        return discarded;
    }
}
