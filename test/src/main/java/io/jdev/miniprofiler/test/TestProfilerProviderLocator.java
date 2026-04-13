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

package io.jdev.miniprofiler.test;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerProviderLocator;

import java.util.Optional;

/**
 * A controllable {@link ProfilerProviderLocator} for testing. Consumers opt in
 * by adding a {@code META-INF/services/io.jdev.miniprofiler.ProfilerProviderLocator}
 * file pointing to this class on their test classpath.
 *
 * <p>Call {@link #activate(ProfilerProvider)} to make {@link #locate()} return
 * the given provider. Call {@link #deactivate()} to make it return empty,
 * allowing the {@link io.jdev.miniprofiler.StaticProfilerProviderLocator}
 * fallback to take effect.</p>
 */
public class TestProfilerProviderLocator implements ProfilerProviderLocator {

    /** Default constructor. */
    public TestProfilerProviderLocator() {
    }

    private static volatile boolean enabled;
    private static volatile ProfilerProvider provider;

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public Optional<ProfilerProvider> locate() {
        if (enabled && provider != null) {
            return Optional.of(provider);
        }
        return Optional.empty();
    }

    /**
     * Activates this locator so that {@link #locate()} returns the given provider.
     *
     * @param profilerProvider the provider to return from {@link #locate()}
     */
    public static void activate(ProfilerProvider profilerProvider) {
        provider = profilerProvider;
        enabled = true;
    }

    /** Deactivates this locator so that {@link #locate()} returns empty. */
    public static void deactivate() {
        enabled = false;
        provider = null;
    }
}
