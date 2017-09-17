/*
 * Copyright 2017 the original author or authors.
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

package io.jdev.miniprofiler.internal

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.Timing
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

import java.util.concurrent.Callable

class TimingImplSpec extends Specification {

    def profiler = new ProfilerImpl("foo", ProfileLevel.Info, new TestProfilerProvider())
    Timing timing = profiler.root

    void "runnable custom timing is profiled properly"() {
        when:
        def called = false
        timing.customTiming('sql', 'query', 'select 1', { ->
            called = true
            Thread.sleep(10)
        } as Runnable)

        then:
        called
        verifyTimings()
    }

    void "runnable custom timing  is profiled properly when it throws an exception"() {
        when:
        timing.customTiming('sql', 'query', 'select 1', { ->
            Thread.sleep(10)
            throw new RuntimeException('eek')
        } as Runnable)

        then:
        def ex = thrown(RuntimeException)
        ex.message == 'eek'
        verifyTimings()
    }

    void "callable custom timing is profiled properly"() {
        when:
        def result = timing.customTiming('sql', 'query', 'select 1', { ->
            Thread.sleep(10)
            'baz'
        } as Callable)

        then:
        result == 'baz'
        verifyTimings()
    }

    void "callable custom timing is profiled properly when it throws an exception"() {
        when:
        timing.customTiming('sql', 'query', 'select 1', { ->
            Thread.sleep(10)
            throw new RuntimeException('eek')
        } as Callable)

        then:
        def ex = thrown(RuntimeException)
        ex.message == 'eek'
        verifyTimings()
    }

    private void verifyTimings() {
        assert timing.customTimings.keySet() == ['sql'] as Set
        def customTiming = timing.customTimings.sql[0]
        assert customTiming.startMilliseconds != null
        assert customTiming.durationMilliseconds >= 10
    }

}
