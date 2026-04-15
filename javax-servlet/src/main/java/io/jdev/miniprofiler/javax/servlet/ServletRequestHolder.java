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

package io.jdev.miniprofiler.javax.servlet;

import javax.servlet.http.HttpServletRequest;

/**
 * Thread-local holder used by {@link ProfilingFilter} to make the in-flight
 * {@link HttpServletRequest} available to {@link ServletUserProvider} during
 * profiling without changing the {@code ProfilerProvider} API.
 */
final class ServletRequestHolder {

    private static final ThreadLocal<HttpServletRequest> CURRENT = new ThreadLocal<>();

    private ServletRequestHolder() {
    }

    static void set(HttpServletRequest request) {
        CURRENT.set(request);
    }

    static void clear() {
        CURRENT.remove();
    }

    static HttpServletRequest current() {
        return CURRENT.get();
    }
}
