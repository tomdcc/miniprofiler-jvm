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
import io.jdev.miniprofiler.storage.jdbc.dialect.MssqlDialect
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Shared

class MssqlStorageIntegrationSpec extends BaseJdbcStorageIntegrationSpec {

    static final int PORT = 1433

    @Shared GenericContainer<?> container
    @Shared HikariDataSource dataSource
    @Shared JdbcStorage mssqlStorage

    JdbcStorage getStorage() { mssqlStorage }

    void setupSpec() {
        container = new GenericContainer<>("mcr.microsoft.com/mssql/server:latest")
            .withExposedPorts(PORT)
            .withEnv("ACCEPT_EULA", "Y")
            .withEnv("MSSQL_SA_PASSWORD", "MiniProfiler1!")
            .waitingFor(Wait.forListeningPort())
        container.start()

        def config = new HikariConfig()
        config.jdbcUrl = "jdbc:sqlserver://${container.host}:${container.getMappedPort(PORT)};encrypt=false"
        config.username = "sa"
        config.password = "MiniProfiler1!"
        dataSource = new HikariDataSource(config)

        mssqlStorage = new JdbcStorage(dataSource, new MssqlDialect())
        mssqlStorage.createTable()
    }

    void cleanupSpec() {
        mssqlStorage?.close()
        dataSource?.close()
        container?.stop()
    }
}
