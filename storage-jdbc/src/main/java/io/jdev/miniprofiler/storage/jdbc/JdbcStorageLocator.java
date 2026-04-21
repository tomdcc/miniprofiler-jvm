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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * {@link StorageLocator} for JDBC storage. Returns an empty {@link Optional} when neither
 * {@code miniprofiler.storage.jdbc.jndiName} nor {@code miniprofiler.storage.jdbc.url} is
 * set, making auto-discovery safe in environments without JDBC storage configuration.
 *
 * <p>When configured, a {@link DataSource} is obtained in this order:</p>
 * <ol>
 *   <li>If {@code jndiName} is set and resolves to a {@link DataSource} via JNDI, that
 *       container-managed source is used and is <em>not</em> owned by the storage.</li>
 *   <li>Otherwise, if {@code url} is set:
 *       <ul>
 *         <li>If HikariCP is on the classpath, a {@link HikariDataSource} is created.</li>
 *         <li>Otherwise, an unpooled {@link DriverManagerDataSource} is created. Users who
 *             want pooling should either add HikariCP to their classpath or construct
 *             {@link JdbcStorage} directly with their own {@link DataSource}.</li>
 *       </ul>
 *       These URL-path data sources are owned by the storage and closed when it closes.</li>
 * </ol>
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
        if (config.getJndiName() != null) {
            DataSource ds = lookupJndi(config.getJndiName());
            if (ds != null) {
                return new DataSourceResult(ds, false);
            }
            // Fall through to URL path if one is configured.
        }
        if (config.getUrl() != null) {
            DataSource ds = isHikariAvailable()
                ? HikariFactory.create(config)
                : new DriverManagerDataSource(config.getUrl(), config.getUsername(), config.getPassword());
            return new DataSourceResult(ds, true);
        }
        return null;
    }

    private static DataSource lookupJndi(String name) {
        try {
            Object obj = new InitialContext().lookup(name);
            return obj instanceof DataSource ? (DataSource) obj : null;
        } catch (NamingException e) {
            return null;
        }
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
        if (config.getUrl() != null) {
            return DatabaseDialect.detect(config.getUrl());
        }
        // JNDI path with no explicit dialect — detect from a live connection.
        try (Connection c = dataSource.getConnection()) {
            return DatabaseDialect.detect(c.getMetaData().getURL());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to detect dialect from JNDI DataSource", e);
        }
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
