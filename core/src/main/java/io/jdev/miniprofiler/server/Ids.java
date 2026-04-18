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


import io.jdev.miniprofiler.ProfilerProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Utility for profiler ID parsing and header construction.
 *
 * <p>If the {@code Accept} header contains {@code application/json}, the UUID is extracted
 * from the JSON request body via {@link ResultsRequest}. Otherwise (or as a fallback) the
 * supplied {@code idParamValue} is parsed as a UUID.</p>
 */
public final class Ids {

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

    /**
     * Builds the value of the {@code X-MiniProfiler-Ids} response header.
     *
     * <p>The header contains the current profiler's ID plus up to
     * {@code provider.getUiConfig().getMaxUnviewedProfiles()} previously-unviewed IDs for the
     * given user. If {@code user} is {@code null} only the current ID is included.</p>
     *
     * @param currentId the ID of the profiler for the current request (always first in the array)
     * @param user      the current user (may be {@code null})
     * @param provider  the profiler provider (used to read config and unviewed storage)
     * @return a JSON array string suitable for use as the {@code X-MiniProfiler-Ids} header value
     */
    public static String buildIdsHeader(UUID currentId, String user, ProfilerProvider provider) {
        Collection<UUID> unviewedIds = user != null
            ? provider.getStorage().getUnviewedIds(user)
            : Collections.emptyList();
        int max = provider.getUiConfig().getMaxUnviewedProfiles();
        return buildIdsHeader(currentId, unviewedIds, max);
    }

    /**
     * Builds the value of the {@code X-MiniProfiler-Ids} response header from pre-fetched data.
     *
     * <p>This overload accepts already-fetched unviewed IDs, allowing callers in async
     * frameworks (e.g.&nbsp;Ratpack) to retrieve them asynchronously before building the header.</p>
     *
     * @param currentId           the ID of the profiler for the current request (always first in the array)
     * @param unviewedIds         previously-unviewed profiler IDs for the current user
     * @param maxUnviewedProfiles the maximum number of unviewed IDs to include
     * @return a JSON array string suitable for use as the {@code X-MiniProfiler-Ids} header value
     */
    public static String buildIdsHeader(UUID currentId, Collection<UUID> unviewedIds, int maxUnviewedProfiles) {
        StringBuilder sb = new StringBuilder("[\"").append(currentId).append('"');
        int count = 0;
        for (UUID uid : unviewedIds) {
            if (count >= maxUnviewedProfiles) {
                break;
            }
            if (!uid.equals(currentId)) {
                sb.append(",\"").append(uid).append('"');
                count++;
            }
        }
        return sb.append(']').toString();
    }

    private Ids() {
    }
}
