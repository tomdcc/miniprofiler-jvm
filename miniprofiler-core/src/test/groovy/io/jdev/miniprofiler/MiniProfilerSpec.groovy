/*
 * Copyright 2013 the original author or authors.
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

import spock.lang.Specification

class MiniProfilerSpec extends Specification {

    def cleanup() {
        MiniProfiler.profilerProvider = null
    }

    void "sets a default profiler if one isn't set when calling start"() {
        given:
        MiniProfiler.profilerProvider = null

        when:
        def profiler = MiniProfiler.start("foo")

        then:
        profiler instanceof ProfilerImpl

        and:
        MiniProfiler.profilerProvider instanceof DefaultProfilerProvider

        and:
        MiniProfiler.currentProfiler == profiler
    }

    void "returns null profiler as current profiler when no profiler provider set"() {
        given:
        MiniProfiler.profilerProvider = null

        when:
        def profiler = MiniProfiler.currentProfiler

        then:
        profiler instanceof NullProfiler

    }

    void "returns current profiler provider's current profiler"() {
        given:
        MiniProfiler.profilerProvider = Mock(ProfilerProvider)
        def profiler = Mock(Profiler)

        when:
        def result = MiniProfiler.currentProfiler

        then:
        1 * MiniProfiler.profilerProvider.getCurrentProfiler() >> profiler

        and:
        result == profiler

    }
}
