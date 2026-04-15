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

package io.jdev.miniprofiler.jakarta.servlet;

import io.jdev.miniprofiler.user.UserProvider;
import io.jdev.miniprofiler.user.UserProviderLocator;

import java.util.Optional;

/**
 * Registers a {@link ServletUserProvider} so that miniprofiler can capture the
 * authenticated servlet user on profiles started inside the request lifecycle.
 *
 * <p>The provider returns {@code null} when no {@link jakarta.servlet.http.HttpServletRequest}
 * is bound to the current thread, allowing later providers in the
 * {@link UserProviderLocator#findUserProvider() chain} to take over for
 * non-request profiling contexts.</p>
 */
public class ServletUserProviderLocator implements UserProviderLocator {

    /** Creates a new instance. */
    public ServletUserProviderLocator() {
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public Optional<UserProvider> locate() {
        return Optional.of(new ServletUserProvider());
    }
}
