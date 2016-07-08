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
import io.jdev.miniprofiler.ProfilerImpl
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

class MiniProfilerRatpackUtilSpec extends Specification {

    def provider = new TestProfilerProvider()

    void "can profile promises from yield"() {
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
            def instrumentedPromise = MiniProfilerRatpackUtil.profileFromYield(profiler, 'foo', Promise.ofLazy { ->
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
}
