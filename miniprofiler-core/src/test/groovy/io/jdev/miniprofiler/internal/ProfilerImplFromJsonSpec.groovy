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

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.sql.NullFormatter
import io.jdev.miniprofiler.sql.SqlFormatterFactory
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class ProfilerImplFromJsonSpec extends Specification {

    def originalFormatter = SqlFormatterFactory.formatter

    void setup() {
        SqlFormatterFactory.formatter = new NullFormatter()
    }

    void cleanup() {
        SqlFormatterFactory.formatter = originalFormatter
    }

    void "round-trip basic profiler with no children or custom timings"() {
        given:
        def profiler = new ProfilerImpl("test-request", ProfileLevel.Info, new TestProfilerProvider())
        profiler.machineName = 'testhost'
        profiler.stop()

        when:
        def json = profiler.asUiJson()
        def restored = ProfilerImpl.fromJson(json)

        then:
        restored.id == profiler.id
        restored.name == profiler.name
        restored.machineName == profiler.machineName
        restored.root.name == profiler.root.name
        restored.root.startMilliseconds == profiler.root.startMilliseconds
        restored.root.durationMilliseconds == profiler.root.durationMilliseconds
        restored.root.children == []
        restored.root.customTimings == null
    }

    void "round-trip profiler with nested child timings"() {
        given:
        def profiler = new ProfilerImpl("test-request", ProfileLevel.Info, new TestProfilerProvider())
        profiler.machineName = 'testhost'
        def step1 = profiler.step('child1')
        def step2 = profiler.step('grandchild')
        step2.stop()
        step1.stop()
        profiler.stop()

        when:
        def json = profiler.asUiJson()
        def restored = ProfilerImpl.fromJson(json)

        then:
        restored.root.children.size() == 1
        restored.root.children[0].name == 'child1'
        restored.root.children[0].durationMilliseconds != null
        restored.root.children[0].children.size() == 1
        restored.root.children[0].children[0].name == 'grandchild'
        restored.root.children[0].children[0].durationMilliseconds != null
    }

    void "round-trip profiler with custom timings"() {
        given:
        def profiler = new ProfilerImpl("test-request", ProfileLevel.Info, new TestProfilerProvider())
        profiler.machineName = 'testhost'
        def step = profiler.step('db-step')
        step.addCustomTiming('sql', 'query', 'select * from people', 5)
        step.stop()
        profiler.stop()

        when:
        def json = profiler.asUiJson()
        def restored = ProfilerImpl.fromJson(json)

        then:
        def restoredStep = restored.root.children[0]
        restoredStep.customTimings.keySet() == ['sql'] as Set
        restoredStep.customTimings['sql'].size() == 1
        restoredStep.customTimings['sql'][0].commandString == 'select * from people'
        restoredStep.customTimings['sql'][0].executeType == 'query'
        restoredStep.customTimings['sql'][0].durationMilliseconds == 5
    }

    void "deserialized profiler produces valid UI json"() {
        given:
        def profiler = new ProfilerImpl("test-request", ProfileLevel.Info, new TestProfilerProvider())
        profiler.machineName = 'testhost'
        profiler.step('child').stop()
        profiler.stop()

        when:
        def json = profiler.asUiJson()
        def restored = ProfilerImpl.fromJson(json)
        def reJson = restored.asUiJson()
        def reRestored = ProfilerImpl.fromJson(reJson)

        then:
        reRestored.id == profiler.id
        reRestored.name == profiler.name
        reRestored.root.children.size() == 1
        reRestored.root.children[0].name == 'child'
    }

    void "fromJson preserves started timestamp"() {
        given:
        def profiler = new ProfilerImpl("test-request", ProfileLevel.Info, new TestProfilerProvider())
        profiler.stop()

        when:
        def restored = ProfilerImpl.fromJson(profiler.asUiJson())

        then:
        restored.started == profiler.started
    }

    void "round-trip profiler with multiple custom timing types"() {
        given:
        def profiler = new ProfilerImpl("test-request", ProfileLevel.Info, new TestProfilerProvider())
        def step = profiler.step('step1')
        step.addCustomTiming('sql', 'query', 'select 1', 3)
        step.addCustomTiming('http', 'GET', 'http://example.com', 10)
        step.stop()
        profiler.stop()

        when:
        def restored = ProfilerImpl.fromJson(profiler.asUiJson())

        then:
        def restoredStep = restored.root.children[0]
        restoredStep.customTimings.keySet() == ['sql', 'http'] as Set
        restoredStep.customTimings['sql'][0].commandString == 'select 1'
        restoredStep.customTimings['http'][0].commandString == 'http://example.com'
    }

    void "deserialized timing depth is correct"() {
        given:
        def profiler = new ProfilerImpl("test-request", ProfileLevel.Info, new TestProfilerProvider())
        def child = profiler.step('child')
        def grandchild = profiler.step('grandchild')
        grandchild.stop()
        child.stop()
        profiler.stop()

        when:
        def restored = ProfilerImpl.fromJson(profiler.asUiJson())

        then:
        restored.root.depth == 0
        restored.root.children[0].depth == 1
        restored.root.children[0].children[0].depth == 2
    }

    void "fromJson throws for invalid JSON string"() {
        when:
        ProfilerImpl.fromJson("not valid json")

        then:
        thrown(IllegalArgumentException)
    }

    void "fromJson throws for non-object JSON"() {
        when:
        ProfilerImpl.fromJson("[1, 2, 3]")

        then:
        thrown(IllegalArgumentException)
    }
}
