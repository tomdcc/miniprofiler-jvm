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

package io.jdev.miniprofiler;

import io.jdev.miniprofiler.storage.Storage;

import java.util.UUID;

/**
 * A profiler provider that defers to the passed-in profiler provider.
 */
public abstract class DelegatingProfilerProvider implements ProfilerProvider {

    protected abstract ProfilerProvider getDelegate();

    @Override
    public Profiler start(String rootName) {
        return getDelegate().start(rootName);
    }

    @Override
    public Profiler start(UUID id, String rootName) {
        return getDelegate().start(id, rootName);
    }

    @Override
    public Profiler start(String rootName, ProfileLevel level) {
        return getDelegate().start(rootName, level);
    }

    @Override
    public Profiler start(UUID id, String rootName, ProfileLevel level) {
        return getDelegate().start(id, rootName, level);
    }

    @Override
    public void stopCurrentSession(boolean discardResults) {
        getDelegate().stopCurrentSession(discardResults);
    }

    @Override
    public void stopSession(ProfilerImpl profilingSession, boolean discardResults) {
        getDelegate().stopSession(profilingSession, discardResults);
    }

    @Override
    public Profiler getCurrentProfiler() {
        return getDelegate().getCurrentProfiler();
    }

    @Override
    public boolean hasCurrentProfiler() {
        return getDelegate().hasCurrentProfiler();
    }

    @Override
    public Storage getStorage() {
        return getDelegate().getStorage();
    }

    @Override
    public void setStorage(Storage storage) {
        getDelegate().setStorage(storage);
    }
}
