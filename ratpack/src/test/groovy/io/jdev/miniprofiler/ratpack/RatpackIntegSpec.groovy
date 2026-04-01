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

import io.jdev.miniprofiler.MiniProfiler
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.internal.ProfilerImpl
import ratpack.exec.Blocking
import ratpack.func.Action
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.handling.RequestId
import spock.lang.Ignore
import spock.lang.Specification

import static ratpack.groovy.test.handling.GroovyRequestFixture.handle

class RatpackIntegSpec extends Specification {

    void cleanup() {
        MiniProfiler.setProfilerProvider(null)
    }

    @Ignore("See https://github.com/ratpack/ratpack/issues/1110 - relying on integ-test/ratpack in the meantime")
    void "profiler is bound to current execution context and is available statically"() {
        given: 'provider and interceptor'
        RatpackContextProfilerProvider provider = new RatpackContextProfilerProvider()
        MiniProfilerExecInitializer interceptor = new MiniProfilerExecInitializer(provider)
        MiniProfiler.setProfilerProvider(provider)

        and: 'chain with handlers'
        def handlerChain = { Chain chain ->
            chain.all(new MiniProfilerStartProfilingHandler(provider))
            chain.all(new AssertionHandler(provider: provider))
            chain.all(new ContextHandler())
        } as Action<Chain>

        when: 'call handler'
        def result = handle(handlerChain) {
            registry {
                add(interceptor)
            }
        }

        then: "all good"
        result.calledNext

        and: 'execution has profiler'
        def profiler = result.registry.get(Profiler)

        and: 'has correct execution info'
        profiler.root.children.name == ["handler", "blocking", "staticstep", "then"]

        and: 'has timing info'
        profiler.root.durationMilliseconds != null

        and: 'was propertly cleaned up'
        profiler.head == null
    }

    void "profiler id can be a uuid-formatted request id"() {
        given: 'provider and interceptor'
        RatpackContextProfilerProvider provider = new RatpackContextProfilerProvider()
        MiniProfiler.setProfilerProvider(provider)
        def id = UUID.randomUUID()

        when: 'call handler'
        def result = handle(new MiniProfilerStartProfilingHandler(provider)) {
            header('X-Request-Id', id.toString())
            registry {
                add(RequestId.Generator, RequestId.Generator.header("X-Request-Id"))
            }
        }

        then: "all good"
        result.calledNext

        and: 'execution has profiler'
        def profiler = result.registry.get(Profiler)

        and: 'has provided id'
        profiler.id == id
    }

}

class AssertionHandler implements Handler {

    ProfilerProvider provider

    @Override
    void handle(Context ctx) throws Exception {
        assert ctx.get(Profiler) instanceof ProfilerImpl
        assert MiniProfiler.currentProfiler instanceof ProfilerImpl
        assert ctx.get(Profiler) == MiniProfiler.currentProfiler

        assert provider.currentProfiler == ctx.get(Profiler)

        ctx.next()
    }
}

class ContextHandler implements Handler {
    void handle(Context ctx) throws Exception {
        def profiler = ctx.get(Profiler)
        def step = profiler.step("handler")
        Blocking.get({ ->
            profiler.step("blocking").stop()
            MiniProfiler.currentProfiler.step("staticstep").stop()
            "yay"
        } as ratpack.func.Factory<String>).then({ result ->
            profiler.step("then").stop()
            ctx.next()
        } as Action)

        step.stop()
    }
}
