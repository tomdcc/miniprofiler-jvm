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

package io.jdev.miniprofiler.jakarta.ee

import io.jdev.miniprofiler.ProfilerProvider
import spock.lang.Shared
import spock.lang.Specification

import jakarta.enterprise.inject.se.SeContainer
import jakarta.enterprise.inject.se.SeContainerInitializer

class ProfilingEJBInterceptorIntegrationSpec extends Specification {

    @Shared SeContainer container

    void setupSpec() {
        container = SeContainerInitializer.newInstance()
            .addBeanClasses(
                DefaultCDIProfilerProvider,
                ProfilingEJBInterceptor,
                ProfiledService,
                ProfiledClassLevelService
            )
            .enableInterceptors(ProfilingEJBInterceptor)
            .initialize()
    }

    void cleanupSpec() {
        container?.close()
    }

    void "interceptor is applied to @Profiled bean via CDI"() {
        given:
        def provider = container.select(ProfilerProvider).get()
        def service = container.select(ProfiledService).get()
        def profiler = provider.start("test")

        when:
        service.doWork()

        then: 'interceptor created a timing step'
        profiler.root.children.size() == 1
        profiler.root.children[0].name =~ /ProfiledService.*\.doWork/

        and: 'timing captured a realistic duration'
        profiler.root.children[0].durationMilliseconds >= 50

        cleanup:
        profiler.stop()
    }

    void "interceptor is applied to class-level @Profiled bean via CDI"() {
        given:
        def provider = container.select(ProfilerProvider).get()
        def service = container.select(ProfiledClassLevelService).get()
        def profiler = provider.start("test")

        when:
        service.doWork()

        then: 'interceptor created a timing step'
        profiler.root.children.size() == 1
        profiler.root.children[0].name =~ /ProfiledClassLevelService.*\.doWork/

        and: 'timing captured a realistic duration'
        profiler.root.children[0].durationMilliseconds >= 50

        cleanup:
        profiler.stop()
    }
}
