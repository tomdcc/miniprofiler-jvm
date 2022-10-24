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
import io.jdev.miniprofiler.internal.NullProfiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.exec.internal.DefaultExecController
import ratpack.func.Action
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.groovy.test.handling.GroovyRequestFixture
import ratpack.handling.Handlers
import ratpack.registry.RegistrySpec
import ratpack.test.ApplicationUnderTest
import ratpack.test.exec.ExecHarness
import ratpack.test.exec.internal.DefaultExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static ratpack.groovy.test.handling.GroovyRequestFixture.handle

class MiniProfilerExecInitializerSpec extends Specification {

    TestProfilerProvider provider
    MiniProfilerStartProfilingHandler startProfilerHandler

    @AutoCleanup
    ApplicationUnderTest app

    void setup() {
        provider = new TestProfilerProvider()
        startProfilerHandler = new MiniProfilerStartProfilingHandler(provider)
    }

    private void addToRegistry(GroovyRequestFixture fixture) {
        addToRegistry(fixture.registry)
    }

    private void addToRegistry(RegistrySpec spec) {
        spec.add(new MiniProfilerExecInitializer(provider))
    }

    void "by default initializer does not create a new profiler on execution start"() {
        when: "run handler with initializer"
        handle(Handlers.next(), this.&addToRegistry)

        then: 'no profiler created'
        !provider.hasCurrent()
    }

    void "initializer stops any bound profiler on context close"() {
        when: "run handler with initializer"
        app = GroovyEmbeddedApp.of {
            registryOf(this.&addToRegistry)
            handlers {
                all(startProfilerHandler)
                get { ctx -> ctx.render("ok") }
            }
        }
        def response = app.httpClient.get()

        then:
        response.status.'2xx'

        and: 'profiler created and was stopped but not discarded'
        provider.current
        provider.current.stopped
        !(provider.current instanceof NullProfiler)
        !provider.wasDiscarded()
    }

    void "initializer stops any bound profiler on execution finish when no response"() {
        when: "run handler with initializer"
        ExecHarness.yieldSingle(this.&addToRegistry) { execution ->
            provider.start("foo")
        }

        then: 'profiler created and was stoped but not discarded'
        provider.current
        provider.current.stopped
        !(provider.current instanceof NullProfiler)
        !provider.wasDiscarded()
    }

    void "discards profiler if option was not to store"() {
        when: "run handler with initializer when initializer won't profile"
        ExecHarness.yieldSingle({ it.add(new MiniProfilerExecInitializer(provider, ProfilerStoreOption.DISCARD_RESULTS))}) { execution ->
            provider.start("foo")
        }

        then: 'profiler created but was discarded'
        provider.current
        provider.current.stopped
        !(provider.current instanceof NullProfiler)
        provider.wasDiscarded()
    }

    void "initializer does not add profiler to execution if one is present"() {
        given: 'profiler'
        def profiler = provider.start("already running")

        when: "run handler with initializer on execution which already has a provider"
        handle(Handlers.next(), this.&addToRegistry)

        then: "current profiler is the original one"
        provider.current == profiler
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
