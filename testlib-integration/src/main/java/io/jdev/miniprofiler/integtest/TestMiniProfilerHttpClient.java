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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * HTTP client for testing MiniProfiler handler endpoints.
 *
 * <p>Exposes named methods for each known MiniProfiler handler endpoint, removing the need to
 * construct URLs and headers inline in each test.</p>
 */
public class TestMiniProfilerHttpClient {

    private static final String DEFAULT_PROFILER_PATH = "miniprofiler";

    private final String baseUrl;
    private final String profilerPath;

    /**
     * Creates a client targeting the given base URL with the default profiler path ({@code miniprofiler}).
     *
     * @param baseUrl the base URL of the server under test, e.g. {@code "http://127.0.0.1:8080/myapp/"}
     */
    public TestMiniProfilerHttpClient(String baseUrl) {
        this(baseUrl, DEFAULT_PROFILER_PATH);
    }

    /**
     * Creates a client targeting the given base URL with a custom profiler path.
     *
     * @param baseUrl      the base URL of the server under test
     * @param profilerPath the path prefix for MiniProfiler handler endpoints, e.g. {@code "admin/miniprofiler"}
     */
    public TestMiniProfilerHttpClient(String baseUrl, String profilerPath) {
        this.baseUrl = baseUrl;
        this.profilerPath = profilerPath;
    }

    /**
     * Sends a GET request to {@code baseUrl + path}.
     *
     * @param path path relative to the base URL, e.g. {@code ""} for the root, {@code "page"} for {@code /page}
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse get(String path) throws IOException {
        return get(path, Collections.<String, String>emptyMap());
    }

    /**
     * Sends a GET request to {@code baseUrl + path} with the given request headers.
     *
     * @param path path relative to the base URL
     * @param headers the extra request headers
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse get(String path, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        conn.setInstanceFollowRedirects(false);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        int statusCode = conn.getResponseCode();
        InputStream stream = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = stream == null ? "" : readStream(stream);
        return new TestHttpResponse(statusCode, body, conn.getHeaderFields());
    }

    /**
     * Fetches a MiniProfiler result as JSON: {@code GET miniprofiler/results?id=<id>}
     * with {@code Accept: application/json}.
     *
     * @param id the profiler result ID
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse getResultsJson(String id) throws IOException {
        return get(profilerPath + "/results?id=" + id, Collections.singletonMap("Accept", "application/json"));
    }

    /**
     * Fetches a MiniProfiler result as HTML: {@code GET <profilerPath>/results?id=<id>}.
     *
     * @param id the profiler result ID
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse getResultsHtml(String id) throws IOException {
        return get(profilerPath + "/results?id=" + id);
    }

    /**
     * Fetches the full results list: {@code GET <profilerPath>/results-list}.
     *
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse getResultsList() throws IOException {
        return get(profilerPath + "/results-list");
    }

    /**
     * Fetches results after a given profile ID: {@code GET <profilerPath>/results-list?last-id=<lastId>}.
     *
     * @param lastId the last seen ID for pagination
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse getResultsList(String lastId) throws IOException {
        return get(profilerPath + "/results-list?last-id=" + lastId);
    }

    /**
     * Fetches the results index page: {@code GET <profilerPath>/results-index}.
     *
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse getResultsIndex() throws IOException {
        return get(profilerPath + "/results-index");
    }

    /**
     * Fetches a static resource: {@code GET <profilerPath>/<name>}.
     *
     * @param name the resource file name
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse getStaticResource(String name) throws IOException {
        return get(profilerPath + "/" + name);
    }

    private static String readStream(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = stream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
