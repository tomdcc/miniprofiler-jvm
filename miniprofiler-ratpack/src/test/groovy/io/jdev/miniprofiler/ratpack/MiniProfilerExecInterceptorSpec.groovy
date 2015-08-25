/*
 * Copyright 2015 the original author or authors.
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

package io.jdev.miniprofiler.ratpack

import io.jdev.miniprofiler.NullProfiler
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.exec.ExecInterceptor
import ratpack.exec.Execution
import ratpack.func.Action
import ratpack.handling.Handler
import ratpack.test.handling.RequestFixture
import spock.lang.Specification

class MiniProfilerExecInterceptorSpec extends Specification {

    final String requestUri = "/foo"

    TestProfilerProvider provider

    void setup() {
        provider = new TestProfilerProvider()
    }

    void "interceptor creates new profiler and binds provider to execution"() {
        when: "run handler with interceptor"
        def result = RequestFixture.handle({ ctx -> ctx.next() } as Handler, { RequestFixture req ->
            req.uri(requestUri)
            req.registry.add(new MiniProfilerExecInterceptor(provider))
        } as Action)

        then: 'profiler provider info attached to execution'
        provider == result.registry.get(ProfilerProvider)

        and: 'has a profiler'
        def profiler = provider.currentProfiler
        profiler != null

        and: 'profiler has current request uri as name'
        profiler.root.name == requestUri
    }

    void "interceptor binds provider to execution even when not profiling"() {
        when: "run handler with interceptor when interceptor won't profile"
        def result = RequestFixture.handle({ ctx -> ctx.next() } as Handler, { RequestFixture req ->
            req.uri(requestUri)
            req.registry.add(new MiniProfilerExecInterceptor(provider) {
                @Override
                protected boolean shouldProfile(Execution execution, ExecInterceptor.ExecType execType) {
                    false
                }
            })
        } as Action)

        then: 'profiler provider info attached to execution'
        provider == result.registry.get(ProfilerProvider)

        and: 'does NOT have a profiler'
        provider.currentProfiler instanceof NullProfiler
    }

    void "interceptor does not add provider to execution if one is present, uses that instead"() {
        given: 'different provider'
        TestProfilerProvider provider2 = new TestProfilerProvider()

        when: "run handler with interceptor on execution which already has a provider"
        def result = RequestFixture.handle({ ctx -> ctx.next() } as Handler, { RequestFixture req ->
            req.uri(requestUri)
            req.registry.add({ execution, execType, cont ->
                execution.add(ProfilerProvider, provider2)
                new MiniProfilerExecInterceptor(provider).intercept(execution, execType, cont)
            } as ExecInterceptor)
        } as Action)

        then: 'profiler provider info attached to execution is the original one'
        provider2 == result.registry.get(ProfilerProvider)

        and: "interceptor's own provider does NOT have a profiler"
        provider.currentProfiler instanceof NullProfiler

        and: 'provider in registry does, though'
        def profiler = provider2.currentProfiler
        profiler.root.name == requestUri
    }

    void "interceptor does not add profiler to execution if one is present"() {
        given: 'profiler'
        def profiler = provider.start("already running")

        when: "run handler with interceptor on execution which already has a provider"
        RequestFixture.handle({ ctx -> ctx.next() } as Handler, { RequestFixture req ->
            req.uri(requestUri)
            req.registry.add(new MiniProfilerExecInterceptor(provider))
        } as Action)

        then: "current profiler is the original one"
        provider.currentProfiler == profiler
    }


}
