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
import org.eclipse.jetty.security.ConstraintMapping
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.security.UserStore
import org.eclipse.jetty.security.authentication.BasicAuthenticator
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.security.Constraint
import org.eclipse.jetty.util.security.Password

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

    private final DefaultProfilerProvider profilerProvider
    private final Server server
    private final String testUser

    InProcessJetty9(String testUser = null) {
        this.testUser = testUser
        profilerProvider = new DefaultProfilerProvider()
        if (testUser != null) {
            profilerProvider.setUserProvider { testUser }
        }
        server = new Server(0)
        new ServletContextHandler(server, '/', false, false).tap {
            securityHandler = buildSecurityHandler()
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
            addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                        throws IOException {
                    profilerProvider.current().step('secure step').close()
                    resp.setContentType('text/plain')
                    resp.writer.write("hello ${req.remoteUser}")
                }
            }), '/secure/hello')
            addServlet(DefaultServlet, '/')
            addFilter(
                new FilterHolder(new ProfilingFilter(profilerProvider: profilerProvider)),
                '/*',
                EnumSet.of(DispatcherType.REQUEST)
            )
        }
        server.start()
    }

    private static ConstraintSecurityHandler buildSecurityHandler() {
        def loginService = new HashLoginService('miniprofiler-test')
        def userStore = new UserStore()
        userStore.addUser('alice', new Password('secret'), ['user'] as String[])
        loginService.userStore = userStore

        def constraint = new Constraint()
        constraint.name = Constraint.__BASIC_AUTH
        constraint.roles = ['user'] as String[]
        constraint.authenticate = true

        def mapping = new ConstraintMapping()
        mapping.constraint = constraint
        mapping.pathSpec = '/secure/*'

        new ConstraintSecurityHandler().tap {
            authenticator = new BasicAuthenticator()
            realmName = 'miniprofiler-test'
            it.loginService = loginService
            constraintMappings = [mapping]
        }
    }

    @Override
    String getTestUser() { testUser }

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
