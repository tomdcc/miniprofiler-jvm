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

package io.jdev.miniprofiler.viewer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.jdev.miniprofiler.DefaultProfilerProvider;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerUiConfig;
import io.jdev.miniprofiler.internal.Pages;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.internal.ResultsRequest;
import io.jdev.miniprofiler.storage.Storage;
import io.jdev.miniprofiler.util.ResourceHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class MiniProfilerViewerServer implements AutoCloseable {

    static final String PREFIX = "miniprofiler";

    private final HttpServer server;

    public MiniProfilerViewerServer(Storage storage) throws IOException {
        ProfilerUiConfig uiConfig = ProfilerUiConfig.defaults();
        uiConfig.setPath("/" + PREFIX);

        DefaultProfilerProvider provider = new DefaultProfilerProvider();
        provider.setStorage(storage);
        provider.setUiConfig(uiConfig);

        server = HttpServer.create(new InetSocketAddress(0), 0);

        ResourceHelper resourceHelper = new ResourceHelper();

        server.createContext("/" + PREFIX + "/results", exchange -> {
            try {
                handleResults(exchange, storage, provider);
            } catch (Exception e) {
                sendError(exchange, 500);
            }
        });

        server.createContext("/" + PREFIX + "/", exchange -> {
            try {
                handleResource(exchange, resourceHelper);
            } catch (Exception e) {
                sendError(exchange, 500);
            }
        });

        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static void handleResults(HttpExchange exchange, Storage storage,
            ProfilerProvider provider) throws IOException {
        String method = exchange.getRequestMethod();
        if (!method.equals("GET") && !method.equals("POST")) {
            sendError(exchange, 405);
            return;
        }

        boolean jsonRequest = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Accept"))
            .map(h -> h.contains("application/json")).orElse(false);

        UUID id = null;

        if (jsonRequest) {
            byte[] body = readAllBytes(exchange.getRequestBody());
            if (body.length > 0) {
                try {
                    id = ResultsRequest.from(new String(body, StandardCharsets.UTF_8)).id;
                } catch (IllegalArgumentException ignored) {
                    // fall through to query param
                }
            }
        }

        if (id == null) {
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("id=")) {
                        try {
                            id = UUID.fromString(param.substring(3));
                        } catch (IllegalArgumentException ignored) {
                            // fall through
                        }
                        break;
                    }
                }
            }
        }

        if (id == null) {
            sendError(exchange, 400);
            return;
        }

        ProfilerImpl profiler = storage.load(id);
        if (profiler == null) {
            sendError(exchange, 404);
            return;
        }

        if (jsonRequest) {
            byte[] response = profiler.asUiJson().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        } else {
            String html = Pages.renderSingleResultPage(profiler, provider, Optional.empty());
            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    private static void handleResource(HttpExchange exchange, ResourceHelper resourceHelper) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String resourceName = path.substring(("/" + PREFIX + "/").length());

        ResourceHelper.Resource resource = resourceName.isEmpty() ? null : resourceHelper.getResource(resourceName);
        if (resource == null) {
            sendError(exchange, 404);
            return;
        }

        byte[] content = resource.getContent();
        exchange.getResponseHeaders().set("Content-Type", resource.getContentType());
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
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

    private static void sendError(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, 0);
        exchange.getResponseBody().close();
    }
}
