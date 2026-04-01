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

package io.jdev.miniprofiler.server;


import java.util.UUID;

/**
 * Utility for parsing a profiler result UUID from an HTTP request.
 *
 * <p>If the {@code Accept} header contains {@code application/json}, the UUID is extracted
 * from the JSON request body via {@link ResultsRequest}. Otherwise (or as a fallback) the
 * supplied {@code idParamValue} is parsed as a UUID.</p>
 */
public final class IdParser {

    /**
     * Parses a profiler UUID from an HTTP request.
     *
     * @param acceptHeader value of the {@code Accept} request header (may be {@code null})
     * @param body         request body as a string (may be {@code null} or empty)
     * @param idParamValue value of the {@code id} request parameter (may be {@code null})
     * @return the parsed UUID, or {@code null} if none could be extracted
     */
    public static UUID parseId(String acceptHeader, String body, String idParamValue) {
        boolean jsonRequest = acceptHeader != null && acceptHeader.contains("application/json");
        UUID id = null;
        if (jsonRequest && body != null && !body.isEmpty()) {
            try {
                id = ResultsRequest.from(body).id;
            } catch (IllegalArgumentException ignored) {
                // fall through to id param
            }
        }
        if (id == null && idParamValue != null && !idParamValue.isEmpty()) {
            try {
                id = UUID.fromString(idParamValue);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return id;
    }

    private IdParser() {
    }
}
