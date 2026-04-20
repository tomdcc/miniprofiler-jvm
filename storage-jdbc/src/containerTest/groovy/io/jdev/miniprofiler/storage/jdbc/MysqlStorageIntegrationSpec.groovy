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
import io.jdev.miniprofiler.storage.jdbc.dialect.MysqlDialect
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import spock.lang.Shared

class MysqlStorageIntegrationSpec extends BaseJdbcStorageIntegrationSpec {

    static final int PORT = 3306

    @Shared GenericContainer<?> container
    @Shared HikariDataSource dataSource
    @Shared JdbcStorage mysqlStorage

    JdbcStorage getStorage() { mysqlStorage }

    void setupSpec() {
        container = new GenericContainer<>("mysql:latest")
            .withExposedPorts(PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withEnv("MYSQL_DATABASE", "miniprofiler")
            .waitingFor(Wait.forListeningPort())
        container.start()

        def config = new HikariConfig()
        config.jdbcUrl = "jdbc:mysql://${container.host}:${container.getMappedPort(PORT)}/miniprofiler"
        config.username = "root"
        config.password = "test"
        dataSource = new HikariDataSource(config)

        mysqlStorage = new JdbcStorage(dataSource, new MysqlDialect())
        mysqlStorage.createTable()
    }

    void cleanupSpec() {
        mysqlStorage?.close()
        dataSource?.close()
        container?.stop()
    }
}
