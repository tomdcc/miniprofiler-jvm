/*
 * Copyright 2013-2026 the original author or authors.
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

import java.util.Optional;

/**
 * Fallback {@link ProfilerProviderLocator} that always returns a
 * {@link StaticProfilerProvider}, delegating to whatever has been configured
 * on {@link MiniProfiler}.
 */
public class StaticProfilerProviderLocator implements ProfilerProviderLocator {

    /** Creates a new static profiler provider locator. */
    public StaticProfilerProviderLocator() {
    }

    @Override
    public int getOrder() {
        return MINIPROFILER_STATIC_LOCATOR_ORDER;
    }

    @Override
    public Optional<ProfilerProvider> locate() {
        return Optional.of(new StaticProfilerProvider());
    }
}
