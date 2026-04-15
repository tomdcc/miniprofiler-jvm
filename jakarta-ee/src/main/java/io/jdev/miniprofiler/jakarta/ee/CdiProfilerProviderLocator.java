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

package io.jdev.miniprofiler.jakarta.ee;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerProviderLocator;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Optional;

/**
 * {@link ProfilerProviderLocator} that looks up a {@link ProfilerProvider} from the
 * Jakarta CDI container using {@link CDI#current()}. Returns an empty {@link Optional}
 * if the Jakarta CDI API is not available or no {@link ProfilerProvider} bean is registered.
 */
public class CdiProfilerProviderLocator implements ProfilerProviderLocator {

    /** Default constructor. */
    public CdiProfilerProviderLocator() {
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public Optional<ProfilerProvider> locate() {
        try {
            CDI<Object> cdi = CDI.current();
            Instance<ProfilerProvider> instance = cdi.select(ProfilerProvider.class);
            if (instance.isResolvable()) {
                return Optional.of(instance.get());
            }
            return Optional.empty();
        } catch (Exception | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }
}
