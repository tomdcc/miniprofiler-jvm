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

package io.jdev.miniprofiler.javax.ee

import io.jdev.miniprofiler.ProfilerProvider
import spock.lang.Shared
import spock.lang.Specification

import javax.enterprise.inject.se.SeContainer
import javax.enterprise.inject.se.SeContainerInitializer

class CdiProfilerProviderLocatorIntegrationSpec extends Specification {

    @Shared SeContainer container

    void setupSpec() {
        container = SeContainerInitializer.newInstance()
            .addBeanClasses(DefaultCdiProfilerProvider)
            .initialize()
    }

    void cleanupSpec() {
        container?.close()
    }

    void "locator finds ProfilerProvider from CDI container"() {
        when:
        def result = new CdiProfilerProviderLocator().locate()

        then:
        result.present
        result.get() instanceof ProfilerProvider
    }

    void "locator returns the CDI-managed singleton ProfilerProvider"() {
        given:
        def locatorProvider = new CdiProfilerProviderLocator().locate().get()
        def containerProvider = container.select(ProfilerProvider).get()

        when:
        def profiler = locatorProvider.start("test")

        then: 'both references share the same underlying CDI bean'
        containerProvider.current() == profiler

        cleanup:
        locatorProvider.stopCurrentSession(true)
    }
}
