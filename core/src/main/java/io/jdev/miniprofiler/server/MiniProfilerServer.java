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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.Storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Embedded HTTP server that handles all MiniProfiler UI endpoints, backed by a
 * {@link ProfilerProvider}.
 *
 * <p>Registers the following endpoints under the path configured in
 * {@link io.jdev.miniprofiler.ProfilerUiConfig#getPath()}:</p>
 * <ul>
 *   <li>{@code {path}/results} — GET or POST; returns profile JSON or an HTML single-result page
 *       depending on the {@code Accept} header.</li>
 *   <li>{@code {path}/results-index} — GET; returns an HTML list-of-results page.</li>
 *   <li>{@code {path}/results-list} — GET; returns a JSON array of results, with optional
 *       {@code ?last-id=} pagination.</li>
 *   <li>{@code {path}/} — GET; serves static MiniProfiler resources (JS, CSS).</li>
 * </ul>
 *
 * <p>An optional {@link Consumer}{@code <HttpServer>} customizer may be supplied; it is invoked
 * on the underlying {@link HttpServer} <em>before</em> the server starts, allowing callers to
 * register additional contexts (e.g. a root redirect or a custom index page).</p>
 *
 * <p>Call {@link #close()} to stop the server.</p>
 */
public class MiniProfilerServer implements AutoCloseable {

    private final ProfilerProvider provider;
    private final HttpServer httpServer;
    private final ResourceHelper resourceHelper = new ResourceHelper();

    /**
     * Creates and starts a server on a random available port.
     *
     * @param provider the profiler provider supplying UI config, storage, and profiler results
     * @throws IOException if the server cannot be started
     */
    public MiniProfilerServer(ProfilerProvider provider) throws IOException {
        this(provider, null);
    }

    /**
     * Creates and starts a server on a random available port, applying the given customizer
     * to the underlying {@link HttpServer} before it starts.
     *
     * @param provider   the profiler provider supplying UI config, storage, and profiler results
     * @param customizer optional consumer called on the {@link HttpServer} before start;
     *                   use it to register additional contexts
     * @throws IOException if the server cannot be started
     */
    public MiniProfilerServer(ProfilerProvider provider, Consumer<HttpServer> customizer) throws IOException {
        this.provider = provider;
        String path = provider.getUiConfig().getPath();
        String basePath = path.endsWith("/") ? path : path + "/";

        httpServer = HttpServer.create(new InetSocketAddress(0), 0);

        httpServer.createContext(basePath + "results", exchange -> {
            try {
                handleResults(exchange);
            } catch (Exception e) {
                sendError(exchange, 500);
            }
        });

        httpServer.createContext(basePath + "results-index", exchange -> {
            try {
                handleResultsIndex(exchange);
            } catch (Exception e) {
                sendError(exchange, 500);
            }
        });

        httpServer.createContext(basePath + "results-list", exchange -> {
            try {
                handleResultsList(exchange);
            } catch (Exception e) {
                sendError(exchange, 500);
            }
        });

        httpServer.createContext(basePath, exchange -> {
            try {
                handleResource(exchange);
            } catch (Exception e) {
                sendError(exchange, 500);
            }
        });

        if (customizer != null) {
            customizer.accept(httpServer);
        }

        httpServer.start();
    }

    /**
     * Returns the TCP port this server is listening on.
     *
     * @return the TCP port this server is listening on
     */
    public int getPort() {
        return httpServer.getAddress().getPort();
    }

    /**
     * Returns the base URL for this server, e.g. {@code http://127.0.0.1:12345/}.
     *
     * @return the base URL, e.g. {@code "http://127.0.0.1:12345/"}
     */
    public String getBaseUrl() {
        return "http://127.0.0.1:" + getPort() + "/";
    }

    /** Stops the server immediately. */
    @Override
    public void close() {
        httpServer.stop(0);
    }

    private void handleResults(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!method.equals("GET") && !method.equals("POST")) {
            sendError(exchange, 405);
            return;
        }

        String acceptHeader = exchange.getRequestHeaders().getFirst("Accept");
        boolean jsonRequest = acceptHeader != null && acceptHeader.contains("application/json");

        String body = null;
        if (jsonRequest) {
            byte[] bytes = readAllBytes(exchange.getRequestBody());
            if (bytes.length > 0) {
                body = new String(bytes, StandardCharsets.UTF_8);
            }
        }

        UUID id = IdParser.parseId(acceptHeader, body, extractQueryParam(exchange.getRequestURI().getRawQuery(), "id"));
        if (id == null) {
            sendError(exchange, 400);
            return;
        }

        ProfilerImpl profiler = provider.getStorage().load(id);
        if (profiler == null) {
            sendError(exchange, 404);
            return;
        }

        if (jsonRequest) {
            sendResponse(exchange, 200, "application/json",
                profiler.asUiJson().getBytes(StandardCharsets.UTF_8));
        } else {
            String html = Pages.renderSingleResultPage(profiler, provider, Optional.empty());
            sendResponse(exchange, 200, "text/html; charset=utf-8",
                html.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleResultsIndex(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405);
            return;
        }
        String html = Pages.renderResultListPage(provider, Optional.empty());
        sendResponse(exchange, 200, "text/html; charset=utf-8",
            html.getBytes(StandardCharsets.UTF_8));
    }

    private void handleResultsList(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405);
            return;
        }
        Storage storage = provider.getStorage();
        Collection<UUID> ids = storage.list(100, null, null, Storage.ListResultsOrder.Descending);

        String lastIdParam = extractQueryParam(exchange.getRequestURI().getRawQuery(), "last-id");
        if (lastIdParam != null) {
            try {
                UUID lastId = UUID.fromString(lastIdParam);
                ProfilerImpl lastProfiler = storage.load(lastId);
                if (lastProfiler != null) {
                    long cutoff = lastProfiler.getStarted();
                    ids = ids.stream()
                        .filter(id -> {
                            ProfilerImpl p = storage.load(id);
                            return p != null && p.getStarted() > cutoff;
                        })
                        .collect(Collectors.toList());
                }
            } catch (IllegalArgumentException ignored) {
                // ignore bad last-id
            }
        }

        String json = Pages.renderResultListJson(ids, storage);
        sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    private void handleResource(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String basePath = provider.getUiConfig().getPath();
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        String resourceName = path.substring(basePath.length());

        ResourceHelper.Resource resource = resourceName.isEmpty() ? null
            : resourceHelper.getResource(resourceName);
        if (resource == null) {
            sendError(exchange, 404);
            return;
        }
        sendResponse(exchange, 200, resource.getContentType(), resource.getContent());
    }

    private static String extractQueryParam(String queryString, String paramName) {
        if (queryString == null) {
            return null;
        }
        String prefix = paramName + "=";
        for (String param : queryString.split("&")) {
            if (param.startsWith(prefix)) {
                return param.substring(prefix.length());
            }
        }
        return null;
    }

    private static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    /**
     * Sends an HTTP response with the given status, content type, and body.
     *
     * @param exchange the HTTP exchange to respond to
     * @param status the HTTP status code
     * @param contentType the value of the {@code Content-Type} response header
     * @param body the response body bytes
     * @throws IOException if writing the response fails
     */
    public static void sendResponse(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    /**
     * Sends an HTTP error response with the given status code and no body.
     *
     * @param exchange the HTTP exchange to respond to
     * @param status the HTTP status code
     * @throws IOException if writing the response fails
     */
    public static void sendError(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, 0);
        exchange.getResponseBody().close();
    }

    /**
     * Returns the underlying {@link ProfilerProvider}.
     *
     * @return the underlying {@link ProfilerProvider}
     */
    public ProfilerProvider getProvider() {
        return provider;
    }
}
