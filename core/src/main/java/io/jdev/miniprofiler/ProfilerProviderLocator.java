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

package io.jdev.miniprofiler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * SPI for locating the current {@link ProfilerProvider}. Implementations are
 * discovered via {@link ServiceLoader} and consulted in ascending {@link #getOrder()}
 * order. The first implementation that returns a non-empty {@link Optional} wins.
 *
 * <p>A {@link StaticProfilerProviderLocator} is registered by default in
 * {@code miniprofiler-core} at order 100 as a fallback. CDI-aware implementations
 * in {@code miniprofiler-javax-ee} (order 20) and {@code miniprofiler-jakarta-ee}
 * (order 10) take precedence when present on the classpath.</p>
 */
public interface ProfilerProviderLocator {

    /**
     * Sentinel order value reserved for {@link StaticProfilerProviderLocator}.
     * Any locator returning this value (or higher) is excluded from
     * {@link MiniProfiler}'s own bootstrap scan to avoid a bootstrap cycle.
     */
    int MINIPROFILER_STATIC_LOCATOR_ORDER = Integer.MAX_VALUE;

    /**
     * Returns the order of this locator. Lower values are consulted first.
     */
    int getOrder();

    /**
     * Attempts to locate a {@link ProfilerProvider}. Returns an empty
     * {@link Optional} if this locator is not applicable in the current environment.
     */
    Optional<ProfilerProvider> locate();

    /**
     * Discovers all registered {@link ProfilerProviderLocator} implementations via
     * {@link ServiceLoader}, sorts them by {@link #getOrder()}, and returns the
     * result of the first one that returns a non-empty {@link Optional}.
     *
     * <p>Falls back to a new {@link StaticProfilerProvider} if no locator succeeds,
     * though in practice the {@link StaticProfilerProviderLocator} always succeeds.</p>
     */
    static ProfilerProvider findProvider() {
        List<ProfilerProviderLocator> locators = new ArrayList<>();
        for (ProfilerProviderLocator locator : ServiceLoader.load(ProfilerProviderLocator.class)) {
            locators.add(locator);
        }
        locators.sort(Comparator.comparingInt(ProfilerProviderLocator::getOrder));
        for (ProfilerProviderLocator locator : locators) {
            Optional<ProfilerProvider> provider = locator.locate();
            if (provider.isPresent()) {
                return provider.get();
            }
        }
        throw new IllegalStateException("No ProfilerProviderLocator returned a provider. Ensure miniprofiler-core is on the classpath.");
    }
}
