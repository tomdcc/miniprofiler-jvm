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

package io.jdev.miniprofiler.internal


import com.fasterxml.jackson.databind.ObjectMapper
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.ProfilerProvider
import spock.lang.Specification

class CustomTimingImplSpec extends Specification {

    ProfilerImpl profiler = new ProfilerImpl("hi there", ProfileLevel.Info, Mock(ProfilerProvider))
    TimingImpl timing = profiler.root as TimingImpl

    void "calculates start milliseconds correctly when duration set initially"() {
        when: 'create query timing with duration'
        def ct = CustomTimingImpl.forDurationFrom(timing, "query", "select * from foo", 5, profiler.started + 77)

        then:
        ct.startMilliseconds == 72
        ct.durationMilliseconds == 5
    }

    void "calculates start milliseconds correctly when duration not set initially"() {
        given:
        when: 'create query timing'
        def ct = CustomTimingImpl.from(timing, "query", "select * from foo", profiler.started + 77)
        ct.stop(profiler.started + 83)

        then:
        ct.startMilliseconds == 77
        ct.durationMilliseconds == 6
    }

    void "json is in expected order"() {
        given:
        def ct = CustomTimingImpl.from(timing, "query", "select * from foo", profiler.started + 77)

        when:
        def parsed = new ObjectMapper().readTree(ct.toJSONString())

        then:
        parsed.fieldNames().collect() ==  [
            'Id',
            'ExecuteType',
            'CommandString',
            'StartMilliseconds',
            'DurationMilliseconds',
            'StackTraceSnippet'
        ]
    }
}
