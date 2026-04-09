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
import spock.lang.Specification

import jakarta.enterprise.context.spi.CreationalContext
import jakarta.enterprise.inject.spi.Bean
import jakarta.enterprise.inject.spi.BeanManager

class CdiProfilerProviderLocatorSpec extends Specification {

    CdiProfilerProviderLocator locator = new CdiProfilerProviderLocator()

    void "locator has order 10"() {
        expect:
        locator.order == 10
    }

    void "locator returns empty optional when CDI is not available"() {
        // In a plain unit test environment there is no JNDI BeanManager
        when:
        def result = locator.locate()

        then:
        !result.present
    }

    void "locator returns empty optional when JNDI returns a BeanManager of the wrong type"() {
        given: 'a locator that simulates finding a javax BeanManager instead of jakarta'
        def wrongTypeLocator = new CdiProfilerProviderLocator() {
            @Override
            Object lookupBeanManagerFromJndi() {
                return "not a jakarta BeanManager" // simulates javax BeanManager on classpath
            }
        }

        when:
        def result = wrongTypeLocator.locate()

        then:
        !result.present
    }

    void "locator returns empty optional when CDI returns wrong ProfilerProvider type"() {
        given: 'a locator that finds a valid BeanManager but the resolved bean is not a ProfilerProvider'
        def bm = Mock(BeanManager)
        def wrongTypeLocator = new CdiProfilerProviderLocator() {
            @Override
            Object lookupBeanManagerFromJndi() { return bm }
        }

        bm.getBeans(ProfilerProvider) >> ([Mock(Bean)] as Set)
        bm.resolve(_) >> Mock(Bean)
        bm.createCreationalContext(_) >> Mock(CreationalContext)
        bm.getReference(_, _, _) >> "not a ProfilerProvider"

        when:
        def result = wrongTypeLocator.locate()

        then:
        !result.present
    }
}
