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

import io.jdev.miniprofiler.ProfileLevel;
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.Timing;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * A profiler implementation which does nothing. Mainly exists
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
 * or not. A {@link NullProfiler} should be returned in those cases.</p>
 */
public class NullProfiler implements Profiler {

    public static final NullProfiler INSTANCE = new NullProfiler();

    private NullProfiler() {
    }

    @Override
    public Timing step(String name) {
        return NullTiming.INSTANCE;
    }

    @Override
    public Timing step(String name, ProfileLevel level) {
        return NullTiming.INSTANCE;
    }

    @Override
    public void step(String name, Runnable block) {
        block.run();
    }

    @Override
    public void step(String name, ProfileLevel level, Runnable block) {
        block.run();
    }

    @Override
    public <T> T step(String name, Callable<T> function) throws Exception {
        return function.call();
    }

    @Override
    public <T> T step(String name, ProfileLevel level, Callable<T> function) throws Exception {
        return function.call();
    }

    @Override
    public void addCustomTiming(String type, String executeType, String command, long duration) {
    }

    @Override
    public void close() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void stop(boolean discardResults) {
    }

    @Override
    public Map<String, Object> toMap() {
        return Collections.emptyMap();
    }

    @Override
    public UUID getId() {
        return null;
    }

    @Override
    public Timing getHead() {
        return NullTiming.INSTANCE;
    }

    @Override
    public Timing getRoot() {
        return NullTiming.INSTANCE;
    }

    @Override
    public void setUser(String user) {
    }

    @Override
    public String getUser() {
        return null;
    }

    @Override
    public Profiler addChild(String name) {
        return INSTANCE;
    }

}
