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

package io.jdev.miniprofiler.jakarta.servlet;

import io.jdev.miniprofiler.user.UserProvider;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * {@link UserProvider} that reads the current user from the in-flight
 * {@link HttpServletRequest}, which {@link ProfilingFilter} stores in a
 * thread-local for the duration of the request.
 *
 * <p>Returns {@link Principal#getName()} from
 * {@link HttpServletRequest#getUserPrincipal()} when present, falling back to
 * {@link HttpServletRequest#getRemoteUser()}, and {@code null} when no request
 * is bound to the current thread or no user has been authenticated.</p>
 */
public class ServletUserProvider implements UserProvider {

    /** Creates a new instance. */
    public ServletUserProvider() {
    }

    @Override
    public String getUser() {
        HttpServletRequest request = ServletRequestHolder.current();
        if (request == null) {
            return null;
        }
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            String name = principal.getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        String remoteUser = request.getRemoteUser();
        if (remoteUser != null && !remoteUser.isEmpty()) {
            return remoteUser;
        }
        return null;
    }
}
