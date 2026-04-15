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

package io.jdev.miniprofiler.user;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * SPI for locating the current {@link UserProvider} implementation. Implementations are
 * discovered via {@link ServiceLoader} and consulted in ascending {@link #getOrder()}
 * order. The first implementation that returns a non-empty {@link Optional} wins.
 *
 * <p>An {@link UnknownUserProviderLocator} is registered by default in
 * {@code miniprofiler-core} at order {@link #UNKNOWN_USER_PROVIDER_LOCATOR_ORDER}
 * as a fallback.</p>
 */
public interface UserProviderLocator {

    /**
     * Sentinel order value reserved for {@link UnknownUserProviderLocator}.
     */
    int UNKNOWN_USER_PROVIDER_LOCATOR_ORDER = Integer.MAX_VALUE;

    /**
     * Returns the order of this locator. Lower values are consulted first.
     *
     * @return the order value; lower values are consulted first
     */
    int getOrder();

    /**
     * Attempts to locate a {@link UserProvider}. Returns an empty
     * {@link Optional} if this locator is not applicable in the current environment.
     *
     * @return an {@link Optional} containing the user provider, or empty if not applicable
     */
    Optional<UserProvider> locate();

    /**
     * Discovers all registered {@link UserProviderLocator} implementations via
     * {@link ServiceLoader}, sorts them by {@link #getOrder()}, and returns the
     * result of the first one that returns a non-empty {@link Optional}.
     *
     * <p>Falls back to an {@link UnknownUserProvider} if no locator succeeds,
     * though in practice the {@link UnknownUserProviderLocator} always succeeds.</p>
     *
     * @return the located {@link UserProvider}
     */
    static UserProvider findUserProvider() {
        List<UserProviderLocator> locators = new ArrayList<>();
        for (UserProviderLocator locator : ServiceLoader.load(UserProviderLocator.class)) {
            locators.add(locator);
        }
        locators.sort(Comparator.comparingInt(UserProviderLocator::getOrder));
        for (UserProviderLocator locator : locators) {
            Optional<UserProvider> provider = locator.locate();
            if (provider.isPresent()) {
                return provider.get();
            }
        }
        throw new IllegalStateException("No UserProviderLocator returned a provider. Ensure miniprofiler-core is on the classpath.");
    }
}
