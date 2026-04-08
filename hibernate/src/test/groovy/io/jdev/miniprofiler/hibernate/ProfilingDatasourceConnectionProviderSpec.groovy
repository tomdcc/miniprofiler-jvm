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
import io.jdev.miniprofiler.sql.ProfilingSpyLogDelegator
import io.jdev.miniprofiler.sql.log4jdbc.SpyLogFactory
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class ProfilingDatasourceConnectionProviderSpec extends Specification {

    void cleanup() {
        MiniProfiler.profilerProvider = null
    }

    void "constructor with provider immediately configures SpyLogFactory"() {
        given:
        def profilerProvider = new TestProfilerProvider()

        when:
        new ProfilingDatasourceConnectionProvider(profilerProvider)

        then:
        SpyLogFactory.spyLogDelegator instanceof ProfilingSpyLogDelegator
        SpyLogFactory.spyLogDelegator.@profilerProvider == profilerProvider
    }

    void "no-arg constructor does not configure SpyLogFactory"() {
        given: 'a sentinel delegator set before construction'
        def originalDelegator = SpyLogFactory.spyLogDelegator

        when:
        new ProfilingDatasourceConnectionProvider()

        then: 'SpyLogFactory was not changed by construction'
        SpyLogFactory.spyLogDelegator == originalDelegator
    }

    void "configure sets up SpyLogFactory using ProfilerProviderLocator"() {
        given:
        def testProvider = new TestProfilerProvider()
        MiniProfiler.profilerProvider = testProvider
        def connectionProvider = new ProfilingDatasourceConnectionProvider()

        when: 'configure is called; profiling setup runs before super, which throws without a DataSource'
        connectionProvider.configure([:])

        then: 'Hibernate throws because no DataSource was provided'
        thrown(Exception)

        and: 'SpyLogFactory was configured before the exception'
        SpyLogFactory.spyLogDelegator instanceof ProfilingSpyLogDelegator

        and: 'the delegator uses the provider resolved via the locator (StaticProfilerProvider -> MiniProfiler)'
        def profiler = MiniProfiler.start("test")
        SpyLogFactory.spyLogDelegator.@profilerProvider.current() == profiler

        cleanup:
        MiniProfiler.profilerProvider?.stopCurrentSession(true)
    }

    void "configure does not reconfigure SpyLogFactory when provider was set via constructor"() {
        given:
        def constructorProvider = Mock(ProfilerProvider)
        def connectionProvider = new ProfilingDatasourceConnectionProvider(constructorProvider)
        def delegatorAfterConstruction = SpyLogFactory.spyLogDelegator

        when: 'configure is called; profilingConfigured flag prevents re-setup, then super throws without a DataSource'
        connectionProvider.configure([:])

        then:
        thrown(Exception)

        and: 'delegator is still the one set by the constructor'
        SpyLogFactory.spyLogDelegator == delegatorAfterConstruction
    }
}
