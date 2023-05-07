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

package io.jdev.miniprofiler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

import java.util.concurrent.Callable

class ProfilerImplSpec extends Specification {

    ProfilerImpl profiler = new ProfilerImpl("foo", ProfileLevel.Info, new TestProfilerProvider())

    void "runnable step is profiled properly"() {
        when:
        profiler.step('foo', { -> profiler.step('bar').stop() })

        then:
        profiler.root.children.name == ['foo']
        profiler.root.children[0].children.name == ['bar']
        profiler.root.children.each { assert it.durationMilliseconds != null }
        profiler.root.children[0].children.each { assert it.durationMilliseconds != null }
    }

    void "runnable step is profiled properly when it throws an exception"() {
        when:
        profiler.step('foo', { -> throw new RuntimeException('eek') })

        then:
        def ex = thrown(RuntimeException)
        ex.message == 'eek'
        profiler.root.children.name == ['foo']
        !profiler.root.children[0].children
        profiler.root.children.each { assert it.durationMilliseconds != null }
    }

    void "callable step is profiled properly"() {
        when:
        def result = profiler.step('foo', { ->
            profiler.step('bar').stop()
            "baz"
        } as Callable)

        then:
        result == 'baz'
        profiler.root.children.name == ['foo']
        profiler.root.children[0].children.name == ['bar']
        profiler.root.children.each { assert it.durationMilliseconds != null }
        profiler.root.children[0].children.each { assert it.durationMilliseconds != null }
    }

    void "callable step is profiled properly when it throws an exception"() {
        when:
        profiler.step('foo', { -> throw new RuntimeException('eek') } as Callable)

        then:
        def ex = thrown(RuntimeException)
        ex.message == 'eek'
        profiler.root.children.name == ['foo']
        !profiler.root.children[0].children
        profiler.root.children.each { assert it.durationMilliseconds != null }
    }

    void 'runnable custom timing is passed through to head timing'() {
        given:
        def timing = Mock(TimingInternal)
        profiler.setHead(timing)
        Runnable block = { -> }

        when:
        profiler.customTiming('sql', 'query', 'select 1', block)

        then:
        1 * timing.customTiming('sql', 'query', 'select 1', block)
    }

    void 'runnable custom timing is called directly when no head timing'() {
        given:
        profiler.stop()
        assert !profiler.head
        def calls = 0
        Runnable block = { -> calls++ }

        when:
        profiler.customTiming('sql', 'query', 'select 1', block)

        then:
        calls == 1
    }

    void 'callable custom timing is passed through to head timing'() {
        given:
        def timing = Mock(TimingInternal)
        profiler.setHead(timing)
        def func = { -> 'foo' } as Callable

        when:
        def result = profiler.customTiming('sql', 'query', 'select 1', (Callable) func)

        then:
        result == 'bar'
        1 * timing.customTiming('sql', 'query', 'select 1', (Callable) func) >> 'bar'
    }

    void 'callable custom timing is called directly when no head timing'() {
        given:
        profiler.stop()
        assert !profiler.head
        def calls = 0
        def func = { -> calls++; 'bar' } as Callable

        when:
        def result = profiler.customTiming('sql', 'query', 'select 1', (Callable) func)

        then:
        calls == 1
        result == 'bar'
    }

    void "json is in expected order"() {
        when:
        def parsed = new ObjectMapper().readTree(profiler.asUiJson())

        then:
        parsed.fieldNames().collect() ==  [
            'Id',
            'Name',
            'Started',
            'DurationMilliseconds',
            'MachineName',
            'Root',
            'ClientTimings'
        ]
    }

}
