/*
 * Copyright 2016 the original author or authors.
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
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.exec.Execution
import ratpack.exec.Operation
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.handling.Handler
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

import static ratpack.groovy.test.handling.GroovyRequestFixture.handle

class MiniProfilerRatpackUtilSpec extends Specification {

    def provider = new TestProfilerProvider()

    void "can profile promises from subscription"() {
        given:
        long sleepTime = 100L

        when:
        Long start = null
        Long afterProfiler = null
        Long afterYield = null
        Profiler profiler = null
        def result = ExecHarness.yieldSingle { c ->
            start = System.currentTimeMillis()
            profiler = new ProfilerImpl('root', ProfileLevel.Info, provider)
            def instrumentedPromise = MiniProfilerRatpackUtil.profile(profiler, 'foo', Promise.ofLazy { ->
                afterYield = System.currentTimeMillis()
                Thread.sleep(sleepTime)
                "foo"
            })
            afterProfiler = System.currentTimeMillis()
            Thread.sleep(sleepTime)
            return instrumentedPromise
        }.value

        then:
        result == 'foo'
        afterYield > afterProfiler
        profiler.root.children.name == ['foo']
        profiler.root.children[0].startMilliseconds >= sleepTime
        profiler.root.children[0].durationMilliseconds < 2 * sleepTime
    }

    void "can profile promises from now"() {
        given:
        long sleepTime = 100L

        when:
        Long start = null
        Long afterProfiler = null
        Long afterYield = null
        Profiler profiler = null
        def result = ExecHarness.yieldSingle { c ->
            start = System.currentTimeMillis()
            profiler = new ProfilerImpl('root', ProfileLevel.Info, provider)
            def instrumentedPromise = MiniProfilerRatpackUtil.profileFromNow(profiler, 'foo', Promise.ofLazy { ->
                afterYield = System.currentTimeMillis()
                Thread.sleep(sleepTime)
                "foo"
            })
            afterProfiler = System.currentTimeMillis()
            Thread.sleep(sleepTime)
            return instrumentedPromise
        }.value

        then:
        result == 'foo'
        afterYield > afterProfiler
        profiler.root.children.name == ['foo']
        profiler.root.children[0].startMilliseconds >= 0
        profiler.root.children[0].startMilliseconds <= afterProfiler - start
        profiler.root.children[0].durationMilliseconds >= 2 * sleepTime
    }

    void "can attach child profilers on fork"() {
        given:
        def provider = new RatpackContextProfilerProvider()

        when: "run handler with initializer on handler which forks execution"
        def profiler
        handle({ ctx ->
            profiler = provider.start("request")
            provider.currentProfiler.step('handler') {
                MiniProfilerRatpackUtil.forkChildProfiler(Execution.fork(), "forked execution").start{ execution ->
                    execution.get(Profiler).step('forked') {
                        ctx.next()
                    }
                }
            }
        } as Handler, { registry.add(new MiniProfilerExecInitializer(provider)) })

        then: 'profiler worked as expected'
        profiler.stopped
        !(profiler instanceof NullProfiler)

        and: 'child profilers are attached'
        profiler.root.name == 'request'
        profiler.root.children.name == ['handler']
        profiler.root.childProfilers.root.name == ['⑃ forked execution']
        profiler.root.childProfilers.root.children.name == [['forked']]
    }

    void "can attach child profilers on fork with composed exec starter"() {
        given:
        def provider = new RatpackContextProfilerProvider()

        when: "run handler with initializer on handler which forks execution"
        def profiler
        def composedOnStartCalled = false
        def composedOnStart = { composedOnStartCalled = true } as Action<Execution>
        handle({ ctx ->
            profiler = provider.start("request")
            provider.currentProfiler.step('handler') {
                MiniProfilerRatpackUtil.forkChildProfiler(Execution.fork(), "forked execution", composedOnStart).start(Operation.of {
                    provider.currentProfiler.step('forked') {
                        ctx.next()
                    }
                })
            }
        } as Handler, { registry.add(new MiniProfilerExecInitializer(provider)) })

        then: 'profiler worked as expected'
        profiler.stopped
        !(profiler instanceof NullProfiler)

        and: 'child profilers are attached'
        profiler.root.name == 'request'
        profiler.root.children.name == ['handler']
        profiler.root.childProfilers.root.name == ['⑃ forked execution']
        profiler.root.childProfilers.root.children.name == [['forked']]

        and: 'composed onStart called'
        composedOnStartCalled
    }
}
