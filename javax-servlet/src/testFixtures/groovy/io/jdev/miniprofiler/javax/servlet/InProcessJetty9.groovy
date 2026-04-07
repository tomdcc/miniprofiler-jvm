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

package io.jdev.miniprofiler.javax.servlet

import io.jdev.miniprofiler.DefaultProfilerProvider
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.ScriptTagWriter
import io.jdev.miniprofiler.integtest.InProcessTestedServer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder

import javax.servlet.DispatcherType
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * An {@link InProcessTestedServer} running an embedded Jetty 9 server with a {@code javax.servlet.Filter},
 * using a {@link DefaultProfilerProvider}.
 *
 * <p>The {@code filterFactory} closure is called with the server's {@link DefaultProfilerProvider}
 * and must return a fully configured filter. This keeps the filter construction in the caller
 * (which knows about the specific filter class) while the server owns the provider.</p>
 */
class InProcessJetty9 implements InProcessTestedServer {

    private final DefaultProfilerProvider profilerProvider = new DefaultProfilerProvider()
    private final Server server

    InProcessJetty9() {
        server = new Server(0)
        new ServletContextHandler(server, '/', false, false).tap {
            addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                        throws IOException {
                    def profiler = profilerProvider.current()
                    def child = profiler.step('child step')
                    child.addCustomTiming('sql', 'reader', 'select * from people', 50L)
                    child.stop()
                    def scriptTag = new ScriptTagWriter(profilerProvider).printScriptTag(profiler)
                    resp.setContentType('text/html')
                    resp.writer.write("""\
                        <html><head>${scriptTag}</head><body>
                        <h1>Test Page</h1>
                        <button id="ajax-call" onclick="fetch('/ajax-endpoint').then(r => r.text())">Make AJAX Call</button>
                        </body></html>""".stripIndent())
                }
            }), '/test-page')
            addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                        throws IOException {
                    def profiler = profilerProvider.current()
                    def child = profiler.step('ajax step')
                    child.addCustomTiming('sql', 'reader', 'select * from ajax_data', 30L)
                    child.stop()
                    resp.setContentType('application/json')
                    resp.writer.write('{"status":"ok"}')
                }
            }), '/ajax-endpoint')
            addServlet(DefaultServlet, '/')
            addFilter(
                new FilterHolder(new ProfilingFilter(profilerProvider: profilerProvider)),
                '/*',
                EnumSet.of(DispatcherType.REQUEST)
            )
        }
        server.start()
    }

    @Override
    String getProfiledPagePath() { 'test-page' }

    @Override
    String getAjaxEndpointPath() { 'ajax-endpoint' }

    @Override
    ProfilerProvider getProfilerProvider() {
        profilerProvider
    }

    @Override
    String getServerUrl() {
        "http://127.0.0.1:${server.connectors[0].localPort}/"
    }

    @Override
    void close() {
        server.stop()
    }
}
