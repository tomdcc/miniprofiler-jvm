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
import io.jdev.miniprofiler.storage.MapStorage
import spock.lang.Specification

import javax.enterprise.inject.se.SeContainerInitializer
import java.util.concurrent.atomic.AtomicInteger

class DefaultCdiProfilerProviderIntegrationSpec extends Specification {

    void "CDI container calls @PreDestroy shutdown() which closes the storage"() {
        given: 'a tracking storage that counts close() calls'
        def closeCount = new AtomicInteger(0)
        def trackingStorage = new MapStorage() {
            @Override
            void close() {
                closeCount.incrementAndGet()
            }
        }

        and: 'a CDI SE container with DefaultCdiProfilerProvider'
        def container = SeContainerInitializer.newInstance()
            .addBeanClasses(DefaultCdiProfilerProvider)
            .initialize()

        and: 'the provider bean has the tracking storage injected'
        def provider = container.select(ProfilerProvider).get()
        provider.storage = trackingStorage

        when: 'the container is closed, triggering @PreDestroy'
        container.close()

        then:
        closeCount.get() == 1
    }
}
