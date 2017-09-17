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

package io.jdev.miniprofiler.internal;

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.Timing;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A timing implementation which does nothing. Mainly exists
 * so that {@link io.jdev.miniprofiler.ProfilerProvider#getCurrentProfiler()}
 * won't ever have to return null.
 *
 * <p>This makes it possible to call:</p>
 * <blockquote><pre>
 * try (Timing t = profilerProvider.getCurrentProfiler().step("do my thing")) {
 *     // do stuff here
 * }
 * </pre></blockquote>
 *
 * <p>...without having to worry about whether there is a current profiler
 * or not. A {@link NullTiming} should be returned by the {@link NullProfiler}
 * that is returned in these cases.</p>
 */
class NullTiming implements Timing {

    static final NullTiming INSTANCE = new NullTiming();

    private NullTiming() {
    }

    @Override
    public void close() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public Map<String, Object> toMap() {
        return Collections.emptyMap();
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Timing getParent() {
        return null;
    }

    @Override
    public long getStartMilliseconds() {
        return 0;
    }

    @Override
    public Long getDurationMilliseconds() {
        return null;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public Map<String, List<CustomTiming>> getCustomTimings() {
        return null;
    }

    @Override
    public List<Timing> getChildren() {
        return null;
    }

    @Override
    public void addCustomTiming(String type, String executeType, String command, long duration) {

    }

    @Override
    public void addCustomTiming(String type, CustomTiming qt) {

    }

    @Override
    public Profiler addChildProfiler(String name) {
        return NullProfiler.INSTANCE;
    }

    @Override
    public List<Profiler> getChildProfilers() {
        return Collections.emptyList();
    }

}
