/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdev.miniprofiler.integtest;

import groovy.json.JsonSlurper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A simple HTTP response returned by {@link TestMiniProfilerHttpClient}.
 */
public class TestHttpResponse {

    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    TestHttpResponse(int statusCode, String body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    /** Returns the HTTP status code. */
    public int statusCode() {
        return statusCode;
    }

    /** Returns the response body as a string. */
    public String body() {
        return body;
    }

    /**
     * Returns the first value of the named response header, or empty if not present.
     * Header name matching is case-insensitive.
     */
    public Optional<String> header(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(lower)) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    return Optional.ofNullable(values.get(0));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Parses the response body as JSON using Groovy's {@code JsonSlurper}.
     * Returns a {@code Map} for JSON objects or a {@code List} for JSON arrays.
     */
    public Object bodyAsJson() {
        return new JsonSlurper().parseText(body);
    }

    /** Returns the value of the {@code Content-Type} response header, or empty if not present. */
    public Optional<String> contentType() {
        return header("content-type");
    }

    /**
     * Parses the {@code X-MiniProfiler-Ids} response header and returns all profiler IDs.
     * Throws {@link AssertionError} if the header is absent or malformed.
     */
    public List<String> miniProfilerIds() {
        String headerValue = header("X-MiniProfiler-Ids")
            .orElseThrow(() -> new AssertionError("X-MiniProfiler-Ids header not present"));
        String trimmed = headerValue.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new AssertionError("X-MiniProfiler-Ids header is not a JSON array: " + headerValue);
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<String>();
        for (String part : inner.split(",")) {
            String id = part.trim();
            if (id.startsWith("\"") && id.endsWith("\"")) {
                id = id.substring(1, id.length() - 1);
            }
            ids.add(id);
        }
        return ids;
    }

    /**
     * Returns the single profiler ID from the {@code X-MiniProfiler-Ids} response header.
     * Throws {@link AssertionError} if the header is absent, malformed, or contains more than one ID.
     */
    public String miniProfilerId() {
        List<String> ids = miniProfilerIds();
        if (ids.size() != 1) {
            throw new AssertionError("Expected exactly one X-MiniProfiler-Id but got " + ids.size() + ": " + ids);
        }
        return ids.get(0);
    }
}
