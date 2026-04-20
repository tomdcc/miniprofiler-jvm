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

package io.jdev.miniprofiler.storage.jdbc

import spock.lang.Specification

class JdbcStorageLocatorSpec extends Specification {

    def "getOrder returns 100"() {
        expect:
        new JdbcStorageLocator().getOrder() == 100
    }

    def "locate returns empty when url not configured"() {
        given:
        def saved = System.getProperty("miniprofiler.storage.jdbc.url")
        System.clearProperty("miniprofiler.storage.jdbc.url")

        when:
        def result = new JdbcStorageLocator().locate()

        then:
        !result.present

        cleanup:
        if (saved != null) {
            System.setProperty("miniprofiler.storage.jdbc.url", saved)
        }
    }
}
