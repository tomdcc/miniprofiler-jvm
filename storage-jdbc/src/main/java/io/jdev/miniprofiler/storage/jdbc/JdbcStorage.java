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

import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.BaseStorage;
import io.jdev.miniprofiler.storage.jdbc.dialect.DatabaseDialect;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * {@link io.jdev.miniprofiler.storage.Storage} implementation backed by a relational
 * database via JDBC. Stores profiler data in a single table with queryable metadata
 * columns alongside a JSON blob for the full profiler tree.
 *
 * <p>Two construction paths are supported:</p>
 * <ul>
 *   <li><strong>DI/programmatic:</strong> pass a {@link DataSource} (and optionally a
 *       {@link DatabaseDialect} and table name). The storage does <em>not</em> own or
 *       close the data source.</li>
 *   <li><strong>Auto-configured via {@link JdbcStorageLocator}:</strong> a HikariCP pool
 *       is created internally. The storage owns and closes the pool.</li>
 * </ul>
 */
public class JdbcStorage extends BaseStorage {

    /** Default table name used when none is specified. */
    public static final String DEFAULT_TABLE_NAME = "mini_profiler_sessions";

    private final DataSource dataSource;
    private final DatabaseDialect dialect;
    private final String tableName;
    private final boolean ownsDataSource;
    private volatile boolean closed;

    /**
     * Creates a new instance with auto-detected dialect and default table name.
     * The dialect is detected by obtaining a connection from the data source.
     *
     * @param dataSource the data source to use; this instance does <em>not</em> own it
     */
    public JdbcStorage(DataSource dataSource) {
        this(dataSource, detectDialect(dataSource), DEFAULT_TABLE_NAME, false);
    }

    /**
     * Creates a new instance with the given dialect and default table name.
     *
     * @param dataSource the data source to use; this instance does <em>not</em> own it
     * @param dialect    the database dialect
     */
    public JdbcStorage(DataSource dataSource, DatabaseDialect dialect) {
        this(dataSource, dialect, DEFAULT_TABLE_NAME, false);
    }

    /**
     * Creates a new instance with the given dialect and table name.
     *
     * @param dataSource the data source to use; this instance does <em>not</em> own it
     * @param dialect    the database dialect
     * @param tableName  the table name to use
     */
    public JdbcStorage(DataSource dataSource, DatabaseDialect dialect, String tableName) {
        this(dataSource, dialect, tableName, false);
    }

    JdbcStorage(DataSource dataSource, DatabaseDialect dialect, String tableName, boolean ownsDataSource) {
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.tableName = tableName;
        this.ownsDataSource = ownsDataSource;
    }

    @Override
    public void save(ProfilerImpl profiler) {
        String json = profiler.toJSONString();
        Long durationMs = profiler.getRoot().getDurationMilliseconds();
        double duration = durationMs != null ? durationMs.doubleValue() : 0.0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.getSaveSql(tableName))) {
            dialect.bindSaveParameters(ps, profiler.getId(), profiler.getName(),
                new Timestamp(profiler.getStarted()), duration,
                profiler.getUser(), false, profiler.getMachineName(), json);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save profiler " + profiler.getId(), e);
        }
    }

    @Override
    public ProfilerImpl load(UUID id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.getLoadSql(tableName))) {
            dialect.setUuid(ps, 1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString(dialect.getJsonColumnName());
                    return ProfilerImpl.fromJson(json);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load profiler " + id, e);
        }
    }

    // 9999-01-01 00:00:00 UTC — safely within DATETIME range in any timezone
    private static final long MAX_TIMESTAMP_MS = 253370764800000L;

    @Override
    public Collection<UUID> list(int maxResults, Date start, Date finish, ListResultsOrder orderBy) {
        Timestamp startTs = new Timestamp(start != null ? start.getTime() : 0L);
        Timestamp finishTs = new Timestamp(finish != null ? finish.getTime() : MAX_TIMESTAMP_MS);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.getListSql(tableName, orderBy))) {
            dialect.bindListParameters(ps, startTs, finishTs, maxResults);
            try (ResultSet rs = ps.executeQuery()) {
                List<UUID> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(dialect.getUuid(rs, dialect.getProfilerIdColumnName()));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list profilers", e);
        }
    }

    @Override
    public void setViewed(String user, UUID id) {
        if (user == null || id == null) {
            return;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.getSetViewedSql(tableName))) {
            ps.setString(1, user);
            dialect.setUuid(ps, 2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set viewed for " + id, e);
        }
    }

    @Override
    public void setUnviewed(String user, UUID id) {
        if (user == null || id == null) {
            return;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.getSetUnviewedSql(tableName))) {
            ps.setString(1, user);
            dialect.setUuid(ps, 2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set unviewed for " + id, e);
        }
    }

    @Override
    public Collection<UUID> getUnviewedIds(String user) {
        if (user == null) {
            return java.util.Collections.emptyList();
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.getGetUnviewedIdsSql(tableName))) {
            ps.setString(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                List<UUID> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(dialect.getUuid(rs, dialect.getProfilerIdColumnName()));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get unviewed ids for " + user, e);
        }
    }

    @Override
    public void clear() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.getClearSql(tableName))) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear profiler data", e);
        }
    }

    @Override
    public void expireOlderThan(Instant cutoff) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dialect.getExpireSql(tableName))) {
            ps.setTimestamp(1, Timestamp.from(cutoff));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to expire profilers older than " + cutoff, e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (ownsDataSource && dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to close data source", e);
            }
        }
    }

    /**
     * Returns the DDL to create the storage table and its indexes.
     *
     * @return the CREATE TABLE / CREATE INDEX DDL string
     */
    public String getCreateTableSql() {
        return dialect.getCreateTableDdl(tableName);
    }

    /**
     * Creates the storage table and indexes in the database. Safe to call
     * multiple times — uses IF NOT EXISTS semantics.
     */
    public void createTable() {
        String ddl = dialect.getCreateTableDdl(tableName);
        try (Connection conn = dataSource.getConnection()) {
            for (String statement : ddl.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(trimmed)) {
                        ps.execute();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create table " + tableName, e);
        }
    }

    private static DatabaseDialect detectDialect(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            return DatabaseDialect.detect(url);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to detect database dialect from data source", e);
        }
    }
}
