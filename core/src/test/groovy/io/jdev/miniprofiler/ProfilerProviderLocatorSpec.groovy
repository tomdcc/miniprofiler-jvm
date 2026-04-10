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

package io.jdev.miniprofiler

import spock.lang.Specification

class ProfilerProviderLocatorSpec extends Specification {

    void "StaticProfilerProviderLocator has order MINIPROFILER_STATIC_LOCATOR_ORDER"() {
        expect:
        new StaticProfilerProviderLocator().order == ProfilerProviderLocator.MINIPROFILER_STATIC_LOCATOR_ORDER
    }

    void "StaticProfilerProviderLocator returns a StaticProfilerProvider"() {
        when:
        def result = new StaticProfilerProviderLocator().locate()

        then:
        result.present
        result.get() instanceof StaticProfilerProvider
    }

    void "findProvider returns a provider via ServiceLoader"() {
        when:
        def provider = ProfilerProviderLocator.findProvider()

        then:
        provider != null
    }

    void "findProvider returns provider from lowest-order locator"() {
        given: 'a low-order locator returning a specific provider'
        def expectedProvider = Mock(ProfilerProvider)
        def lowOrderLocator = new ProfilerProviderLocator() {
            int getOrder() { return 1 }
            Optional<ProfilerProvider> locate() { Optional.of(expectedProvider) }
        }
        def highOrderLocator = new ProfilerProviderLocator() {
            int getOrder() { return 99 }
            Optional<ProfilerProvider> locate() { throw new AssertionError("should not be called") }
        }

        when:
        def locators = [highOrderLocator, lowOrderLocator].sort { it.order }
        def result = locators.findResult { it.locate().orElse(null) }

        then:
        result == expectedProvider
    }

    void "findProvider skips locators returning empty optional"() {
        given:
        def expectedProvider = Mock(ProfilerProvider)
        def emptyLocator = new ProfilerProviderLocator() {
            int getOrder() { return 1 }
            Optional<ProfilerProvider> locate() { Optional.empty() }
        }
        def successLocator = new ProfilerProviderLocator() {
            int getOrder() { return 2 }
            Optional<ProfilerProvider> locate() { Optional.of(expectedProvider) }
        }

        when:
        def locators = [emptyLocator, successLocator].sort { it.order }
        def result = locators.findResult { it.locate().orElse(null) }

        then:
        result == expectedProvider
    }
}
