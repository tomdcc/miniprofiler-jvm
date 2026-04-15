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

package io.jdev.miniprofiler.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * SPI for locating the current {@link Storage} implementation. Implementations are
 * discovered via {@link ServiceLoader} and consulted in ascending {@link #getOrder()}
 * order. The first implementation that returns a non-empty {@link Optional} wins.
 *
 * <p>A {@link MapStorageLocator} is registered by default in
 * {@code miniprofiler-core} at order {@link #MAP_STORAGE_LOCATOR_ORDER} as a fallback.</p>
 */
public interface StorageLocator {

    /**
     * Sentinel order value reserved for {@link MapStorageLocator}.
     */
    int MAP_STORAGE_LOCATOR_ORDER = Integer.MAX_VALUE;

    /**
     * Returns the order of this locator. Lower values are consulted first.
     *
     * @return the order value; lower values are consulted first
     */
    int getOrder();

    /**
     * Attempts to locate a {@link Storage} implementation. Returns an empty
     * {@link Optional} if this locator is not applicable in the current environment.
     *
     * @return an {@link Optional} containing the storage, or empty if not applicable
     */
    Optional<Storage> locate();

    /**
     * Discovers all registered {@link StorageLocator} implementations via
     * {@link ServiceLoader}, sorts them by {@link #getOrder()}, and returns the
     * result of the first one that returns a non-empty {@link Optional}.
     *
     * <p>Falls back to a new {@link MapStorage} if no locator succeeds,
     * though in practice the {@link MapStorageLocator} always succeeds.</p>
     *
     * @return the located {@link Storage}
     */
    static Storage findStorage() {
        List<StorageLocator> locators = new ArrayList<>();
        for (StorageLocator locator : ServiceLoader.load(StorageLocator.class)) {
            locators.add(locator);
        }
        locators.sort(Comparator.comparingInt(StorageLocator::getOrder));
        for (StorageLocator locator : locators) {
            Optional<Storage> storage = locator.locate();
            if (storage.isPresent()) {
                return storage.get();
            }
        }
        throw new IllegalStateException("No StorageLocator returned a storage. Ensure miniprofiler-core is on the classpath.");
    }
}
