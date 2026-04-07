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

package io.jdev.miniprofiler.ratpack

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.ScriptTagWriter
import io.jdev.miniprofiler.integtest.InProcessTestedServer
import io.jdev.miniprofiler.internal.ProfilerImpl
import ratpack.func.Action
import ratpack.guice.Guice
import ratpack.server.RatpackServer
import ratpack.server.RatpackServerSpec

/**
 * An {@link InProcessTestedServer} running an embedded Ratpack server.
 */
class InProcessRatpack implements InProcessTestedServer {

    private final RatpackServer server

    InProcessRatpack() {
        server = RatpackServer.start(configure())
    }

    protected Action<? super RatpackServerSpec> configure() {
        { spec ->
            spec.registry(Guice.registry { bindings ->
                bindings.module(MiniProfilerModule)
            })
            spec.handlers { chain ->
                chain.get('test-page') { ctx ->
                    def provider = ctx.get(ProfilerProvider)
                    def profiler = new ProfilerImpl('/test-page', ProfileLevel.Info, provider)
                    def child = profiler.step('child step')
                    child.addCustomTiming('sql', 'reader', 'select * from people', 50L)
                    child.stop()
                    profiler.stop()
                    provider.storage.save(profiler)
                    def scriptTag = new ScriptTagWriter(provider).printScriptTag(profiler)
                    def html = """\
                        <html><head>${scriptTag}</head><body>
                        <h1>Test Page</h1>
                        <button id="ajax-call" onclick="fetch('/ajax-endpoint').then(r => r.text())">Make AJAX Call</button>
                        </body></html>""".stripIndent()
                    ctx.response.send('text/html; charset=utf-8', html)
                }
                chain.prefix('ajax-endpoint') { ajaxChain ->
                    ajaxChain.insert(MiniProfilerStartProfilingHandlers)
                    ajaxChain.get { ctx ->
                        def profiler = ctx.get(ProfilerProvider).current()
                        def child = profiler.step('ajax step')
                        child.addCustomTiming('sql', 'reader', 'select * from ajax_data', 30L)
                        child.stop()
                        ctx.response.send('application/json', '{"status":"ok"}')
                    }
                }
                chain.prefix(MiniProfilerHandlerChain.DEFAULT_PREFIX, MiniProfilerHandlerChain)
            }
        }
    }

    @Override
    String getProfiledPagePath() { 'test-page' }

    @Override
    String getAjaxEndpointPath() { 'ajax-endpoint' }

    @Override
    ProfilerProvider getProfilerProvider() {
        server.registry.get().get(ProfilerProvider)
    }

    @Override
    String getServerUrl() {
        "http://127.0.0.1:${server.bindPort}/"
    }

    @Override
    void close() {
        server.stop()
    }
}
