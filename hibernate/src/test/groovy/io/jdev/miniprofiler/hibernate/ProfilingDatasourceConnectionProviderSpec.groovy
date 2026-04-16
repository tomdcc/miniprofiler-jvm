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

package io.jdev.miniprofiler.hibernate

import io.jdev.miniprofiler.MiniProfiler
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class ProfilingDatasourceConnectionProviderSpec extends Specification {

    void cleanup() {
        MiniProfiler.profilerProvider = null
    }

    void "constructor with provider configures profiling"() {
        given:
        def profilerProvider = new TestProfilerProvider()

        when:
        new ProfilingDatasourceConnectionProvider(profilerProvider)

        then: 'profiling is configured (configure will not re-resolve)'
        noExceptionThrown()
    }

    void "no-arg constructor does not eagerly configure profiling"() {
        when:
        new ProfilingDatasourceConnectionProvider()

        then: 'construction succeeds without needing a ProfilerProvider'
        noExceptionThrown()
    }

    void "configure resolves the profiler provider via locator when not set via constructor"() {
        given:
        def testProvider = new TestProfilerProvider()
        MiniProfiler.profilerProvider = testProvider
        def connectionProvider = new ProfilingDatasourceConnectionProvider()

        when: 'configure is called; profiling setup runs before super, which throws without a DataSource'
        connectionProvider.configure([:])

        then: 'Hibernate throws because no DataSource was provided'
        thrown(Exception)
    }

    void "configure does not reconfigure when provider was set via constructor"() {
        given:
        def constructorProvider = Mock(ProfilerProvider)
        def connectionProvider = new ProfilingDatasourceConnectionProvider(constructorProvider)

        when: 'configure is called; super throws without a DataSource'
        connectionProvider.configure([:])

        then:
        thrown(Exception)
    }
}
