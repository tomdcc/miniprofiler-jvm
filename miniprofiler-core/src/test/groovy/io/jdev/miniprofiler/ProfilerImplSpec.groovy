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

package io.jdev.miniprofiler

import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

import java.util.concurrent.Callable

class ProfilerImplSpec extends Specification {

    def profiler = new ProfilerImpl("foo", ProfileLevel.Info, new TestProfilerProvider())

    void "runnable is profiled properly"() {
        when:
        profiler.step('foo', { -> profiler.step('bar').stop() })

        then:
        profiler.root.children.name == ['foo']
        profiler.root.children[0].children.name == ['bar']
        profiler.root.children.each { assert it.durationMilliseconds != null }
        profiler.root.children[0].children.each { assert it.durationMilliseconds != null }
    }

    void "runnable is profiled properly when it throws an exception"() {
        when:
        profiler.step('foo', { -> throw new RuntimeException('eek') })

        then:
        def ex = thrown(RuntimeException)
        ex.message == 'eek'
        profiler.root.children.name == ['foo']
        !profiler.root.children[0].children
        profiler.root.children.each { assert it.durationMilliseconds != null }

    }

    void "callable is profiled properly"() {
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

    void "callable is profiled properly when it throws an exception"() {
        when:
        profiler.step('foo', { -> throw new RuntimeException('eek') } as Callable)

        then:
        def ex = thrown(RuntimeException)
        ex.message == 'eek'
        profiler.root.children.name == ['foo']
        !profiler.root.children[0].children
        profiler.root.children.each { assert it.durationMilliseconds != null }

    }

}
