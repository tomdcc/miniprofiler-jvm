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
import io.jdev.miniprofiler.storage.jdbc.dialect.OracleDialect
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Shared

import java.time.Duration

class OracleStorageIntegrationSpec extends BaseJdbcStorageIntegrationSpec {

    static final int PORT = 1521

    @Shared GenericContainer<?> container
    @Shared HikariDataSource dataSource
    @Shared JdbcStorage oracleStorage

    JdbcStorage getStorage() { oracleStorage }

    void setupSpec() {
        container = new GenericContainer<>(System.getProperty("dockerImage.oracle-free"))
            .withExposedPorts(PORT)
            .withEnv("ORACLE_PASSWORD", "test")
            .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!.*\\n", 1)
                .withStartupTimeout(Duration.ofMinutes(5)))
        container.start()

        def config = new HikariConfig()
        config.jdbcUrl = "jdbc:oracle:thin:@${container.host}:${container.getMappedPort(PORT)}/FREEPDB1"
        config.username = "system"
        config.password = "test"
        dataSource = new HikariDataSource(config)

        oracleStorage = new JdbcStorage(dataSource, new OracleDialect())
        oracleStorage.createTable()
    }

    void cleanupSpec() {
        oracleStorage?.close()
        dataSource?.close()
        container?.stop()
    }
}
