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

import io.jdev.miniprofiler.DefaultProfilerProvider;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerUiConfig;
import io.jdev.miniprofiler.server.MiniProfilerServer;
import io.jdev.miniprofiler.storage.Storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MiniProfilerViewerServer implements AutoCloseable {

    static final String DEFAULT_PREFIX = "/miniprofiler";

    private final MiniProfilerServer server;

    public MiniProfilerViewerServer(Storage storage) throws IOException {
        this(makeProvider(storage));
    }

    private static ProfilerProvider makeProvider(Storage storage) {
        ProfilerUiConfig uiConfig = ProfilerUiConfig.defaults();
        uiConfig.setPath(DEFAULT_PREFIX);

        DefaultProfilerProvider provider = new DefaultProfilerProvider();
        provider.setStorage(storage);
        provider.setUiConfig(uiConfig);

        return provider;
    }

    public MiniProfilerViewerServer(ProfilerProvider provider) throws IOException {
        String indexPath = provider.getUiConfig().getPath() + "/results-index";
        server = new MiniProfilerServer(provider, httpServer ->
            httpServer.createContext("/", exchange -> {
                try {
                    if (!exchange.getRequestMethod().equals("GET")) {
                        MiniProfilerServer.sendError(exchange, 405);
                        return;
                    }
                    exchange.getResponseHeaders().set("Location", indexPath);
                    byte[] body = ("Redirecting to " + indexPath).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(301, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.getResponseBody().close();
                } catch (Exception e) {
                    MiniProfilerServer.sendError(exchange, 500);
                }
            })
        );
    }

    public int getPort() {
        return server.getPort();
    }

    @Override
    public void close() {
        server.close();
    }
}
