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

import io.jdev.miniprofiler.user.UserProvider;
import io.jdev.miniprofiler.user.UserProviderLocator;

import jakarta.enterprise.inject.spi.CDI;
import java.util.Optional;

/**
 * Registers a {@link CdiPrincipalUserProvider} so that miniprofiler can capture the
 * caller principal from CDI in jakarta EE deployments. Returns an empty
 * {@link Optional} if the jakarta CDI API is not available on the classpath.
 *
 * <p>Ordered after the servlet provider so that the request-bound provider wins on
 * request threads in mixed deployments; this provider takes over for
 * non-request contexts (EJB timers, {@code @Asynchronous}, etc.) via the chain in
 * {@link UserProviderLocator#findUserProvider()}.</p>
 */
public class CdiPrincipalUserProviderLocator implements UserProviderLocator {

    /** Creates a new instance. */
    public CdiPrincipalUserProviderLocator() {
    }

    /** {@inheritDoc} Returns {@code 40}. */
    @Override
    public int getOrder() {
        return 40;
    }

    @Override
    public Optional<UserProvider> locate() {
        try {
            // Trigger class loading to confirm the jakarta CDI API is available; the actual
            // CDI.current() call is deferred to UserProvider.getUser() so that a deployment
            // without an active container at locator time still works.
            Class.forName(CDI.class.getName());
            return Optional.of(new CdiPrincipalUserProvider());
        } catch (Exception | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }
}
