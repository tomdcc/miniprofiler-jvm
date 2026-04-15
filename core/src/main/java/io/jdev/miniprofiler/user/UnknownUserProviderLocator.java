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

package io.jdev.miniprofiler.user;

import java.util.Optional;

/**
 * Fallback {@link UserProviderLocator} that always returns an
 * {@link UnknownUserProvider}.
 */
public class UnknownUserProviderLocator implements UserProviderLocator {

    /** Creates a new instance. */
    public UnknownUserProviderLocator() {
    }

    @Override
    public int getOrder() {
        return UNKNOWN_USER_PROVIDER_LOCATOR_ORDER;
    }

    @Override
    public Optional<UserProvider> locate() {
        return Optional.of(UnknownUserProvider.INSTANCE);
    }
}
