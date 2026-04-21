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

import java.sql.SQLException

/**
 * Runs against every HikariCP major supported by the locator. Verifies that
 * {@link JdbcStorageLocator} discovers the URL-configured storage, that the internal
 * DataSource is a HikariCP pool, and that closing the storage closes the pool.
 */
class JdbcStorageLocatorHikariCrossVersionSpec extends Specification {

    def "locator produces a Hikari-backed storage"() {
        given:
        def url = "jdbc:h2:mem:xvtest-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        System.setProperty("miniprofiler.storage.jdbc.url", url)

        when:
        def result = new JdbcStorageLocator().locate()

        then:
        result.present

        when:
        def storage = (JdbcStorage) result.get()
        def dataSource = storage.dataSource

        then:
        dataSource.getClass().getName() == "com.zaxxer.hikari.HikariDataSource"

        when: "the storage is usable against a live database"
        storage.createTable()
        def conn = dataSource.getConnection()

        then:
        conn != null

        cleanup:
        conn?.close()
        storage?.close()
        System.clearProperty("miniprofiler.storage.jdbc.url")
    }

    def "closing the storage shuts the pool down"() {
        given:
        def url = "jdbc:h2:mem:xvtest-close-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        System.setProperty("miniprofiler.storage.jdbc.url", url)
        def storage = (JdbcStorage) new JdbcStorageLocator().locate().get()
        def dataSource = storage.dataSource

        when:
        storage.close()
        dataSource.getConnection()

        then:
        thrown(SQLException)

        cleanup:
        System.clearProperty("miniprofiler.storage.jdbc.url")
    }
}
