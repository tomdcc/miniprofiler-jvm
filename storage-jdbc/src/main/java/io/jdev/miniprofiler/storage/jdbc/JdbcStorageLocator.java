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

import javax.sql.DataSource;
import java.util.Optional;

/**
 * {@link StorageLocator} for JDBC storage. Returns an empty {@link Optional} when the
 * {@code miniprofiler.storage.jdbc.url} property is not set, making auto-discovery
 * safe in environments without JDBC storage configuration.
 *
 * <p>When configured, a {@link DataSource} is obtained as follows:</p>
 * <ol>
 *   <li>If HikariCP is on the classpath, a {@link HikariDataSource} is created.</li>
 *   <li>Otherwise, an unpooled {@link DriverManagerDataSource} is created. Users who
 *       want pooling should either add HikariCP to their classpath or construct
 *       {@link JdbcStorage} directly with their own {@link DataSource}.</li>
 * </ol>
 *
 * <p>The resulting {@link DataSource} is owned by the {@link JdbcStorage} instance and
 * closed when the storage is closed.</p>
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
            DataSourceResult result = createDataSource(config);
            if (result == null) {
                return Optional.empty();
            }
            DatabaseDialect dialect = resolveDialect(config, result.dataSource);
            String tableName = config.getTable() != null
                ? config.getTable()
                : JdbcStorage.DEFAULT_TABLE_NAME;
            return Optional.of(new JdbcStorage(result.dataSource, dialect, tableName, result.ownsDataSource));
        } catch (Exception | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }

    private static final class DataSourceResult {
        final DataSource dataSource;
        final boolean ownsDataSource;

        DataSourceResult(DataSource dataSource, boolean ownsDataSource) {
            this.dataSource = dataSource;
            this.ownsDataSource = ownsDataSource;
        }
    }

    private static DataSourceResult createDataSource(JdbcStorageConfig config) {
        if (config.getUrl() != null) {
            DataSource ds = isHikariAvailable()
                ? HikariFactory.create(config)
                : new DriverManagerDataSource(config.getUrl(), config.getUsername(), config.getPassword());
            return new DataSourceResult(ds, true);
        }
        return null;
    }

    private static boolean isHikariAvailable() {
        try {
            Class.forName("com.zaxxer.hikari.HikariDataSource", false,
                JdbcStorageLocator.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static DatabaseDialect resolveDialect(JdbcStorageConfig config, DataSource dataSource) {
        if (config.getDialect() != null) {
            return DatabaseDialect.forName(config.getDialect());
        }
        return DatabaseDialect.detect(config.getUrl());
    }

    private static final class HikariFactory {
        static DataSource create(JdbcStorageConfig config) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getUrl());
            if (config.getUsername() != null) {
                hikariConfig.setUsername(config.getUsername());
            }
            if (config.getPassword() != null) {
                hikariConfig.setPassword(config.getPassword());
            }
            return new HikariDataSource(hikariConfig);
        }
    }
}
