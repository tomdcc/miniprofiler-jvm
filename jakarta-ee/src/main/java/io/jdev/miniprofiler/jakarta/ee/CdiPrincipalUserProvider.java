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

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import java.security.Principal;

/**
 * {@link UserProvider} that resolves the current user from the CDI built-in
 * {@link Principal} bean. Works in any context where CDI exposes a caller
 * principal (servlet, JAX-RS, EJB, async EJB).
 *
 * <p>Returns {@code null} when CDI is not available, when no Principal bean
 * is resolvable in the current context, or when the resolved principal name
 * is empty (some containers return placeholder names like
 * {@code "ANONYMOUS"} for unauthenticated callers, which are also treated as
 * no user).</p>
 */
public class CdiPrincipalUserProvider implements UserProvider {

    private static final String ANONYMOUS_PRINCIPAL = "ANONYMOUS";

    /** Creates a new instance. */
    public CdiPrincipalUserProvider() {
    }

    @Override
    public String getUser() {
        try {
            CDI<Object> cdi = CDI.current();
            Instance<Principal> instance = cdi.select(Principal.class);
            if (instance.isUnsatisfied() || instance.isAmbiguous()) {
                return null;
            }
            Principal principal = instance.get();
            if (principal == null) {
                return null;
            }
            String name = principal.getName();
            if (name == null || name.isEmpty() || ANONYMOUS_PRINCIPAL.equalsIgnoreCase(name)) {
                return null;
            }
            return name;
        } catch (Exception | NoClassDefFoundError e) {
            return null;
        }
    }
}
