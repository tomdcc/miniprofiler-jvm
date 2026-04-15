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

class StorageLocatorSpec extends Specification {

    void "MapStorageLocator has order MAP_STORAGE_LOCATOR_ORDER"() {
        expect:
        new MapStorageLocator().order == StorageLocator.MAP_STORAGE_LOCATOR_ORDER
    }

    void "MapStorageLocator returns a MapStorage"() {
        when:
        def result = new MapStorageLocator().locate()

        then:
        result.present
        result.get() instanceof MapStorage
    }

    void "findStorage returns a storage via ServiceLoader"() {
        when:
        def storage = StorageLocator.findStorage()

        then:
        storage != null
    }

    void "findStorage returns storage from lowest-order locator"() {
        given: 'a low-order locator returning a specific storage'
        def expectedStorage = Mock(Storage)
        def lowOrderLocator = new StorageLocator() {
            int getOrder() { return 1 }
            Optional<Storage> locate() { Optional.of(expectedStorage) }
        }
        def highOrderLocator = new StorageLocator() {
            int getOrder() { return 99 }
            Optional<Storage> locate() { throw new AssertionError("should not be called") }
        }

        when:
        def locators = [highOrderLocator, lowOrderLocator].sort { it.order }
        def result = locators.findResult { it.locate().orElse(null) }

        then:
        result == expectedStorage
    }

    void "findStorage skips locators returning empty optional"() {
        given:
        def expectedStorage = Mock(Storage)
        def emptyLocator = new StorageLocator() {
            int getOrder() { return 1 }
            Optional<Storage> locate() { Optional.empty() }
        }
        def successLocator = new StorageLocator() {
            int getOrder() { return 2 }
            Optional<Storage> locate() { Optional.of(expectedStorage) }
        }

        when:
        def locators = [emptyLocator, successLocator].sort { it.order }
        def result = locators.findResult { it.locate().orElse(null) }

        then:
        result == expectedStorage
    }
}
