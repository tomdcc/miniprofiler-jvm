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
     * Polls the results JSON endpoint until the profiler with the given ID is available.
     *
     * @param id the profiler result ID
     * @return the response with status 200, or the last 404 response if the timeout expires
     * @throws IOException on network error
     * @see #awaitInResultsList(String)
     */
    public TestHttpResponse awaitResultsJson(String id) throws IOException {
        return awaitResults(() -> getResultsJson(id));
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
     * Polls the results HTML endpoint until the profiler with the given ID is available.
     *
     * @param id the profiler result ID
     * @return the response with status 200, or the last 404 response if the timeout expires
     * @throws IOException on network error
     * @see #awaitInResultsList(String)
     */
    public TestHttpResponse awaitResultsHtml(String id) throws IOException {
        return awaitResults(() -> getResultsHtml(id));
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
     * Polls the results list until an entry with the given ID appears, or the timeout expires.
     *
     * <p>The profiler is saved to storage after the HTTP response is sent, so there is a brief
     * window where the client has received the profiler ID header but the profile is not yet
     * in the results list. This method handles that race by polling.</p>
     *
     * @param id the profiler ID to wait for
     * @return the matching entry as a {@code Map}, or {@code null} if not found within the timeout
     * @throws IOException on network error
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> awaitInResultsList(String id) throws IOException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            java.util.List<Map<String, Object>> list =
                (java.util.List<Map<String, Object>>) getResultsList().bodyAsJson();
            for (Map<String, Object> entry : list) {
                if (id.equals(entry.get("Id"))) {
                    return entry;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
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

    /**
     * POSTs client-side performance data to {@code <profilerPath>/results} with
     * {@code Accept: application/json} and {@code Content-Type: application/json}.
     *
     * @param id             the profiler result ID
     * @param performanceJson JSON array string for the {@code Performance} field, or {@code null} to omit it
     * @return the response
     * @throws IOException on network error
     */
    public TestHttpResponse postResultsJson(String id, String performanceJson) throws IOException {
        String body = performanceJson != null
            ? "{\"Id\":\"" + id + "\",\"Performance\":" + performanceJson + "}"
            : "{\"Id\":\"" + id + "\"}";
        return post(profilerPath + "/results", body,
            Collections.singletonMap("Accept", "application/json"));
    }

    private TestHttpResponse post(String path, String body, Map<String, String> headers) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        conn.getOutputStream().write(bodyBytes);
        int statusCode = conn.getResponseCode();
        InputStream stream = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String responseBody = stream == null ? "" : readStream(stream);
        return new TestHttpResponse(statusCode, responseBody, conn.getHeaderFields());
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

    private TestHttpResponse awaitResults(IOSupplier<TestHttpResponse> fetcher) throws IOException {
        long deadline = System.currentTimeMillis() + 5_000;
        TestHttpResponse last = fetcher.get();
        while (last.statusCode() == 404 && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return last;
            }
            last = fetcher.get();
        }
        return last;
    }

    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws IOException;
    }
}
