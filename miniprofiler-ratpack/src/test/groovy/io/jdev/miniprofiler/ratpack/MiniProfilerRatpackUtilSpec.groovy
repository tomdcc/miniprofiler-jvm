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

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.internal.NullProfiler
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.exec.Blocking
import ratpack.exec.ExecInitializer
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.test.ApplicationUnderTest
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class MiniProfilerRatpackUtilSpec extends Specification {

    def polling = new PollingConditions()

    def provider = new TestProfilerProvider()

    @AutoCleanup
    ApplicationUnderTest app

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

        and: "handler with initializer on handler which forks execution"
        def profiler
        app = GroovyEmbeddedApp.of {
            registryOf {
                add(ProfilerProvider, provider)
                add(ExecInitializer, new MiniProfilerExecInitializer(provider))
            }
            handlers {
                all(new MiniProfilerStartProfilingHandler(provider))
                get { ctx ->
                    profiler = provider.current
                    provider.current.step('handler') {
                        MiniProfilerRatpackUtil.forkChildProfiler(Execution.fork(), "forked execution").start { execution ->
                            provider.current.step('forked') {
                            }
                        }
                    }
                    Blocking.op {
                        // give some time for the forked exec to finish
                        Thread.sleep(100)
                    }.then {
                        ctx.render("ok")
                    }
                }
            }
        }

        when:
        def response = app.httpClient.get()

        then:
        response.status.'2xx'

        and:
        polling.eventually {
            profiler.stopped
        }
        !(profiler instanceof NullProfiler)
        println profiler.toJSONString()

        and: 'child profilers are attached'
        profiler.root.name == '/'
        profiler.root.children.name == ['handler']
        def handlerTiming = profiler.root.children[0]
        handlerTiming.childProfilers.root.name == ['⑃ forked execution']
        handlerTiming.childProfilers.root.children.name == [['forked']]
    }

    void "can attach child profilers on fork with composed exec starter"() {
        given:
        def provider = new RatpackContextProfilerProvider()

        and: "handler with initializer on handler which forks execution"
        def profiler
        def composedOnStartCalled = false
        def composedOnStart = { composedOnStartCalled = true } as Action<Execution>
        app = GroovyEmbeddedApp.of {
            registryOf {
                add(ProfilerProvider, provider)
                add(ExecInitializer, new MiniProfilerExecInitializer(provider))
            }
            handlers {
                all(new MiniProfilerStartProfilingHandler(provider))
                get { ctx ->
                    profiler = provider.current
                    provider.current.step('handler') {
                        MiniProfilerRatpackUtil.forkChildProfiler(Execution.fork(), "forked execution", composedOnStart).start { execution ->
                            provider.current.step('forked') {
                            }
                        }
                    }
                    Blocking.op {
                        // give some time for the forked exec to finish
                        Thread.sleep(100)
                    }.then {
                        ctx.render("ok")
                    }
                }
            }
        }

        when:
        def response = app.httpClient.get()

        then:
        response.status.'2xx'

        and:
        polling.eventually {
            profiler.stopped
        }
        !(profiler instanceof NullProfiler)
        println profiler.toJSONString()

        and: 'child profilers are attached'
        profiler.root.name == '/'
        profiler.root.children.name == ['handler']
        def handlerTiming = profiler.root.children[0]
        handlerTiming.childProfilers.root.name == ['⑃ forked execution']
        handlerTiming.childProfilers.root.children.name == [['forked']]

        and: 'composed onStart called'
        composedOnStartCalled
    }
}
