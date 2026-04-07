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

package io.jdev.miniprofiler.server

import io.jdev.miniprofiler.DefaultProfilerProvider
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.ScriptTagWriter
import io.jdev.miniprofiler.integtest.InProcessTestedServer
import io.jdev.miniprofiler.internal.ProfilerImpl

/**
 * An {@link InProcessTestedServer} backed by the core {@link MiniProfilerServer} (JDK HttpServer),
 * using a {@link DefaultProfilerProvider}.
 */
class InProcessMiniProfilerServer implements InProcessTestedServer {

    private final DefaultProfilerProvider profilerProvider = new DefaultProfilerProvider()
    private final MiniProfilerServer server

    InProcessMiniProfilerServer() {
        server = new MiniProfilerServer(profilerProvider, { httpServer ->
            httpServer.createContext('/test-page') { exchange ->
                if (exchange.requestMethod != 'GET') {
                    MiniProfilerServer.sendError(exchange, 405)
                    return
                }
                def profiler = new ProfilerImpl('/test-page', ProfileLevel.Info, profilerProvider)
                def child = profiler.step('child step')
                child.addCustomTiming('sql', 'reader', 'select * from people', 50L)
                child.stop()
                profiler.stop()
                profilerProvider.storage.save(profiler)
                def scriptTag = new ScriptTagWriter(profilerProvider).printScriptTag(profiler)
                def html = """\
                    <html><head>${scriptTag}</head><body>
                    <h1>Test Page</h1>
                    </body></html>""".stripIndent()
                MiniProfilerServer.sendResponse(exchange, 200, 'text/html; charset=utf-8',
                    html.getBytes('UTF-8'))
            }
        })
    }

    @Override
    String getProfiledPagePath() { 'test-page' }

    @Override
    ProfilerProvider getProfilerProvider() {
        profilerProvider
    }

    @Override
    String getServerUrl() {
        server.baseUrl
    }

    @Override
    void close() {
        server.close()
    }
}
