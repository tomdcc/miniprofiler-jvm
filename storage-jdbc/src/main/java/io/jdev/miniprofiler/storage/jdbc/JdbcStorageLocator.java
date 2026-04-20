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

package io.jdev.miniprofiler.storage.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jdev.miniprofiler.storage.Storage;
import io.jdev.miniprofiler.storage.StorageLocator;
import io.jdev.miniprofiler.storage.jdbc.dialect.DatabaseDialect;

import java.util.Optional;

/**
 * {@link StorageLocator} for JDBC storage. Returns an empty {@link Optional} when the
 * {@code miniprofiler.storage.jdbc.url} property is not set, making auto-discovery
 * safe in environments without JDBC storage configuration.
 *
 * <p>When configured, creates a HikariCP connection pool that is owned by the
 * resulting {@link JdbcStorage} instance and closed when the storage is closed.</p>
 */
public class JdbcStorageLocator implements StorageLocator {

    /** Creates a new instance. */
    public JdbcStorageLocator() {
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public Optional<Storage> locate() {
        try {
            JdbcStorageConfig config = JdbcStorageConfig.create();
            if (!config.isConfigured()) {
                return Optional.empty();
            }

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getUrl());
            if (config.getUsername() != null) {
                hikariConfig.setUsername(config.getUsername());
            }
            if (config.getPassword() != null) {
                hikariConfig.setPassword(config.getPassword());
            }
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);

            DatabaseDialect dialect = config.getDialect() != null
                ? DatabaseDialect.forName(config.getDialect())
                : DatabaseDialect.detect(config.getUrl());
            String tableName = config.getTable() != null
                ? config.getTable()
                : JdbcStorage.DEFAULT_TABLE_NAME;

            return Optional.of(new JdbcStorage(dataSource, dialect, tableName, true));
        } catch (Exception | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }
}
