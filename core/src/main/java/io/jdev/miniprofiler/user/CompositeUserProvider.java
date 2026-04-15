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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A {@link UserProvider} that delegates to an ordered list of providers,
 * returning the first non-null, non-empty result from
 * {@link UserProvider#getUser()}.
 *
 * <p>Used internally by {@link UserProviderLocator#findUserProvider()} to
 * combine providers contributed by all registered locators, so that a
 * provider whose backing context is unavailable (for example a servlet
 * request provider on a non-request thread) can fall through to the next
 * provider in order.</p>
 */
final class CompositeUserProvider implements UserProvider {

    private final List<UserProvider> providers;

    CompositeUserProvider(List<UserProvider> providers) {
        this.providers = Collections.unmodifiableList(Objects.requireNonNull(providers, "providers"));
    }

    @Override
    public String getUser() {
        for (UserProvider provider : providers) {
            String user = provider.getUser();
            if (user != null && !user.isEmpty()) {
                return user;
            }
        }
        return null;
    }

    List<UserProvider> getProviders() {
        return providers;
    }
}
