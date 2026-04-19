/*
 * Copyright 2026 the original author or authors.
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

import groovy.json.JsonSlurper
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class ClientTimingSpec extends Specification {

    def provider = new TestProfilerProvider()

    void "toJson includes Duration for span timings"() {
        given:
        def ct = new ClientTiming("fetchStart", 0L, 12L)

        when:
        def json = ct.toJson()

        then:
        json["Name"] == "fetchStart"
        json["Start"] == 0L
        json["Duration"] == 12L
        json.containsKey("Duration")
    }

    void "toJson omits Duration key for point timings"() {
        given:
        def ct = new ClientTiming("firstPaintTime", 380L, null)

        when:
        def json = ct.toJson()

        then:
        json["Name"] == "firstPaintTime"
        json["Start"] == 380L
        !json.containsKey("Duration")
    }

    void "toJSONString serializes span timing correctly"() {
        given:
        def ct = new ClientTiming("fetchStart", 0L, 12L)

        when:
        def parsed = new JsonSlurper().parseText(ct.toJSONString())

        then:
        parsed.Name == "fetchStart"
        parsed.Start == 0
        parsed.Duration == 12
    }

    void "toJSONString omits Duration for point timing"() {
        given:
        def ct = new ClientTiming("firstPaintTime", 380L, null)

        when:
        def parsed = new JsonSlurper().parseText(ct.toJSONString())

        then:
        parsed.Name == "firstPaintTime"
        parsed.Start == 380
        !parsed.containsKey("Duration")
    }

    void "listFromJson with mix of span and point timings parses correctly via profiler round-trip"() {
        given: "a JSON string embedding client timings in a profiler"
        def profiler = new ProfilerImpl("test", ProfileLevel.Info, provider)
        profiler.stop()
        def json = profiler.asUiJson().replace('"ClientTimings":null',
            '"ClientTimings":{"Timings":[' +
                '{"Name":"fetchStart","Start":0,"Duration":12},' +
                '{"Name":"firstPaintTime","Start":380}' +
            ']}')

        when:
        def restored = ProfilerImpl.fromJson(json)

        then:
        def ct = restored.toJson()["ClientTimings"]
        ct != null
        def timings = ct["Timings"]
        timings.size() == 2
        timings[0].name == "fetchStart"
        timings[0].start == 0L
        timings[0].duration == 12L
        timings[1].name == "firstPaintTime"
        timings[1].start == 380L
        timings[1].duration == null
    }

    void "listFromJson returns null when Timings array is absent"() {
        given:
        def profiler = new ProfilerImpl("test", ProfileLevel.Info, provider)
        profiler.stop()

        when:
        def restored = ProfilerImpl.fromJson(profiler.asUiJson())

        then:
        restored.toJson()["ClientTimings"] == null
    }
}
