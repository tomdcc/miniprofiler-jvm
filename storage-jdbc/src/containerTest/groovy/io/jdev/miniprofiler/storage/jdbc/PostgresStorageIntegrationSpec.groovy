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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.jdev.miniprofiler.storage.jdbc.dialect.PostgresDialect
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Shared

class PostgresStorageIntegrationSpec extends BaseJdbcStorageIntegrationSpec {

    static final int PORT = 5432

    @Shared GenericContainer<?> container
    @Shared HikariDataSource dataSource
    @Shared JdbcStorage pgStorage

    JdbcStorage getStorage() { pgStorage }

    void setupSpec() {
        container = new GenericContainer<>("postgres:latest")
            .withExposedPorts(PORT)
            .withEnv("POSTGRES_PASSWORD", "test")
            .withEnv("POSTGRES_DB", "miniprofiler")
            .waitingFor(Wait.forListeningPort())
        container.start()

        def config = new HikariConfig()
        config.jdbcUrl = "jdbc:postgresql://${container.host}:${container.getMappedPort(PORT)}/miniprofiler"
        config.username = "postgres"
        config.password = "test"
        dataSource = new HikariDataSource(config)

        pgStorage = new JdbcStorage(dataSource, new PostgresDialect())
        pgStorage.createTable()
    }

    void cleanupSpec() {
        pgStorage?.close()
        dataSource?.close()
        container?.stop()
    }
}
