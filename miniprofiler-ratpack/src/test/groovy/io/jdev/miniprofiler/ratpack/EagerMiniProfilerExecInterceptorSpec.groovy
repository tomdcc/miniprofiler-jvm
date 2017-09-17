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

import io.jdev.miniprofiler.internal.NullProfiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.func.Action
import ratpack.handling.Handler
import ratpack.test.exec.ExecHarness
import ratpack.test.handling.RequestFixture
import spock.lang.Specification

class EagerMiniProfilerExecInterceptorSpec extends Specification {

    final String requestUri = "/foo"

    TestProfilerProvider provider

    void setup() {
        provider = new TestProfilerProvider()
    }

    void "interceptor creates new profiler and binds provider to execution"() {
        when: "run handler with interceptor"
        ExecHarness.yieldSingle({ it.add(new EagerMiniProfilerExecInitializer(provider))}) { execution ->
            provider.start(requestUri)
        }

        then: 'profiler created and was stoped but not discarded'
        provider.currentProfiler
        provider.currentProfiler.stopped
        !(provider.currentProfiler instanceof NullProfiler)
        !provider.wasDiscarded()
    }

    void "interceptor does not add profiler to execution if one is present"() {
        given: 'profiler'
        def profiler = provider.start("already running")

        when: "run handler with interceptor on execution which already has a provider"
        RequestFixture.handle({ ctx -> ctx.next() } as Handler, { RequestFixture req ->
            req.uri(requestUri)
            req.registry.add(new EagerMiniProfilerExecInitializer(provider))
        } as Action)

        then: "current profiler is the original one"
        provider.currentProfiler == profiler
    }


}
