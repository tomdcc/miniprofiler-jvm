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

import com.google.common.collect.ImmutableList
import io.jdev.miniprofiler.NullProfiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.exec.internal.DefaultExecController
import ratpack.func.Action
import ratpack.handling.Handler
import ratpack.test.exec.ExecHarness
import ratpack.test.exec.internal.DefaultExecHarness
import ratpack.test.handling.RequestFixture
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MiniProfilerExecInitializerSpec extends Specification {

    final String requestUri = "/foo"

    TestProfilerProvider provider
    MiniProfilerStartProfilingHandler startProfilerHandler

    void setup() {
        provider = new TestProfilerProvider()
        startProfilerHandler = new MiniProfilerStartProfilingHandler(provider)
    }

    void "by default initializer does not create a new profiler on execuiton start"() {
        when: "run handler with initializer"
        RequestFixture.handle({ ctx -> ctx.next() } as Handler, { RequestFixture req ->
            req.uri(requestUri)
            req.registry.add(new MiniProfilerExecInitializer(provider))
        } as Action)

        then: 'no profiler created'
        !provider.hasCurrentProfiler()
    }

    @Ignore("See https://github.com/ratpack/ratpack/issues/1110")
    void "initializer stops any bound profiler on response send"() {
        when: "run handler with initializer"
        RequestFixture.handle(startProfilerHandler, { RequestFixture req ->
            req.uri(requestUri)
            req.registry.add(new MiniProfilerExecInitializer(provider))
        } as Action)

        then: 'profiler created and was stoped but not discarded'
        provider.currentProfiler
        provider.currentProfiler.stopped
        !(provider.currentProfiler instanceof NullProfiler)
        !provider.wasDiscarded()
    }

    void "initializer stops any bound profiler on execution finish when no response"() {
        when: "run handler with initializer"
        ExecHarness.yieldSingle({ it.add(new MiniProfilerExecInitializer(provider))}) { execution ->
            provider.start("foo")
        }

        then: 'profiler created and was stoped but not discarded'
        provider.currentProfiler
        provider.currentProfiler.stopped
        !(provider.currentProfiler instanceof NullProfiler)
        !provider.wasDiscarded()
    }

    void "discards profiler if option was not to store"() {
        when: "run handler with initializer when initializer won't profile"
        ExecHarness.yieldSingle({ it.add(new MiniProfilerExecInitializer(provider, ProfilerStoreOption.DISCARD_RESULTS))}) { execution ->
            provider.start("foo")
        }

        then: 'profiler created but was discarded'
        provider.currentProfiler
        provider.currentProfiler.stopped
        !(provider.currentProfiler instanceof NullProfiler)
        provider.wasDiscarded()
    }

    void "initializer does not add profiler to execution if one is present"() {
        given: 'profiler'
        def profiler = provider.start("already running")

        when: "run handler with initializer on execution which already has a provider"
        RequestFixture.handle({ ctx -> ctx.next() } as Handler, { RequestFixture req ->
            req.uri(requestUri)
            req.registry.add(new MiniProfilerExecInitializer(provider))
        } as Action)

        then: "current profiler is the original one"
        provider.currentProfiler == profiler
    }

    void "initializer cleans up profiler exactly once per request"() {
        given: 'initializer that asserts that its completion only happens once'
        Map<Execution, AtomicInteger> completes = new ConcurrentHashMap()
        def initializer = new MiniProfilerExecInitializer(provider) {
            @Override
            protected void executionComplete(Execution execution) {
                completes.computeIfAbsent(execution, { new AtomicInteger() }).incrementAndGet()
                super.executionComplete(execution)
            }
        }
        def execController = new DefaultExecController()
        execController.initializers = ImmutableList.of(initializer)
        def harness = new DefaultExecHarness(execController)

        when: "run handler with initializer on handler which forks execution"
        def latch = new CountDownLatch(3)
        harness.run { e ->
            // do forked execution
            Execution.fork().onComplete { latch.countDown() }.start(Action.noop())
            // and something running in a blocking thread and then completed
            Promise.value("").blockingMap{ v -> v }.then { latch.countDown() }
            latch.countDown()
        }
        harness.close()
        latch.await(2, TimeUnit.SECONDS)

        then: "each execution only completed once"
        completes.values().each {
            assert it.get() == 1
        }
    }


}