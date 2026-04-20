/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdev.miniprofiler.test

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.internal.ProfilerImpl
import spock.lang.Specification

class ProfilerComparatorSpec extends Specification {

    def provider = new TestProfilerProvider()

    void "verify passes when custom links match"() {
        given:
        def profiler = new ProfilerImpl("test", ProfileLevel.Info, provider)
        profiler.addCustomLink('AppStats', 'http://example.com/appstats')
        profiler.addCustomLink('Tracing', 'http://example.com/tracing')
        profiler.stop()

        def expected = ProfilerDsl.profiler('test') { p ->
            p.customLink('AppStats', 'http://example.com/appstats')
            p.customLink('Tracing', 'http://example.com/tracing')
        }

        when:
        ProfilerComparator.verify(profiler, expected)

        then:
        noExceptionThrown()
    }

    void "verify passes when both have no custom links"() {
        given:
        def profiler = new ProfilerImpl("test", ProfileLevel.Info, provider)
        profiler.stop()

        def expected = ProfilerDsl.profiler('test')

        when:
        ProfilerComparator.verify(profiler, expected)

        then:
        noExceptionThrown()
    }

    void "verify fails when custom link URL differs"() {
        given:
        def profiler = new ProfilerImpl("test", ProfileLevel.Info, provider)
        profiler.addCustomLink('AppStats', 'http://example.com/wrong')
        profiler.stop()

        def expected = ProfilerDsl.profiler('test') { p ->
            p.customLink('AppStats', 'http://example.com/appstats')
        }

        when:
        ProfilerComparator.verify(profiler, expected)

        then:
        def e = thrown(AssertionError)
        e.message.contains("custom link URL for 'AppStats'")
    }

    void "verify fails when custom link count differs"() {
        given:
        def profiler = new ProfilerImpl("test", ProfileLevel.Info, provider)
        profiler.addCustomLink('AppStats', 'http://example.com/appstats')
        profiler.addCustomLink('Tracing', 'http://example.com/tracing')
        profiler.stop()

        def expected = ProfilerDsl.profiler('test') { p ->
            p.customLink('AppStats', 'http://example.com/appstats')
        }

        when:
        ProfilerComparator.verify(profiler, expected)

        then:
        def e = thrown(AssertionError)
        e.message.contains("custom link count")
    }

    void "verify fails when expected link is missing from actual"() {
        given:
        def profiler = new ProfilerImpl("test", ProfileLevel.Info, provider)
        profiler.stop()

        def expected = ProfilerDsl.profiler('test') { p ->
            p.customLink('AppStats', 'http://example.com/appstats')
        }

        when:
        ProfilerComparator.verify(profiler, expected)

        then:
        def e = thrown(AssertionError)
        e.message.contains("custom link count")
    }
}
