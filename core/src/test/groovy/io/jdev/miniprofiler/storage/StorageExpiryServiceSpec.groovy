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

package io.jdev.miniprofiler.storage

import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

import static org.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS

class StorageExpiryServiceSpec extends Specification {

    def "calls expireOlderThan on the schedule"() {
        given:
        def cutoffs = new CopyOnWriteArrayList<Instant>()
        def storage = Stub(Storage) {
            expireOlderThan(_ as Instant) >> { Instant cutoff -> cutoffs.add(cutoff) }
        }
        def expiryAge = Duration.ofMillis(500)
        def service = new StorageExpiryService(storage, expiryAge, Duration.ofMillis(100))

        when:
        await().atMost(5, SECONDS).until { cutoffs.size() >= 2 }
        service.close()

        then:
        cutoffs.size() >= 2
        // each cutoff should be approximately now - expiryAge
        cutoffs.every { cutoff ->
            def expected = Instant.now().minus(expiryAge)
            Math.abs(cutoff.toEpochMilli() - expected.toEpochMilli()) < 2000
        }
    }

    def "close stops further calls to expireOlderThan"() {
        given:
        def callCount = new AtomicInteger()
        def storage = Stub(Storage) {
            expireOlderThan(_ as Instant) >> { callCount.incrementAndGet() }
        }
        def service = new StorageExpiryService(storage, Duration.ofMillis(500), Duration.ofMillis(50))

        when:
        await().atMost(5, SECONDS).until { callCount.get() >= 1 }
        service.close()
        int countAfterClose = callCount.get()
        Thread.sleep(200)

        then:
        callCount.get() == countAfterClose
    }

    def "close is idempotent"() {
        given:
        def storage = Stub(Storage)
        def service = new StorageExpiryService(storage, Duration.ofHours(1), Duration.ofHours(1))

        when:
        service.close()
        service.close()

        then:
        noExceptionThrown()
    }
}
