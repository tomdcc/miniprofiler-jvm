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

package io.jdev.miniprofiler.storage.jdbc.dialect;

import io.jdev.miniprofiler.storage.Storage.ListResultsOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Encapsulates database-specific SQL and DDL for JDBC storage.
 *
 * <p>Each supported database has a concrete implementation that generates
 * appropriate SQL for that database's types and syntax. Dialects are
 * auto-detected from the JDBC URL via {@link #detect(String)}, or can
 * be looked up by name via {@link #forName(String)}.</p>
 */
public interface DatabaseDialect {

    /**
     * Returns the CREATE TABLE and CREATE INDEX DDL statements for the given table name.
     * Statements are separated by semicolons for display and manual execution.
     *
     * @param tableName the table name
     * @return the DDL string (multiple statements separated by semicolons)
     */
    String getCreateTableDdl(String tableName);

    /**
     * Executes the DDL to create the storage table and indexes. The default
     * implementation splits {@link #getCreateTableDdl} on semicolons and
     * executes each statement. Dialects that need different execution
     * (e.g. PL/SQL blocks) should override this method.
     *
     * @param conn      the database connection
     * @param tableName the table name
     * @throws SQLException if a database access error occurs
     */
    default void executeCreateTable(Connection conn, String tableName) throws SQLException {
        String ddl = getCreateTableDdl(tableName);
        for (String statement : ddl.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(trimmed)) {
                    ps.execute();
                }
            }
        }
    }

    /**
     * Returns the INSERT SQL that avoids duplicates on the profiler_id column.
     * Parameters are bound by {@link #bindSaveParameters}.
     *
     * @param tableName the table name
     * @return the SQL string
     */
    String getSaveSql(String tableName);

    /**
     * Binds parameters for the save SQL returned by {@link #getSaveSql}.
     * The default implementation binds 8 parameters in standard order.
     * Dialects that need additional parameters (e.g. MSSQL's
     * {@code WHERE NOT EXISTS} clause) should override this method.
     *
     * @param ps                   the prepared statement
     * @param profilerId           the profiler UUID
     * @param name                 the session name
     * @param started              the start timestamp
     * @param durationMilliseconds the duration
     * @param userName             the user name
     * @param hasUserViewed        the viewed flag
     * @param machineName          the machine name
     * @param profileJson          the serialised JSON
     * @throws SQLException if a database access error occurs
     */
    default void bindSaveParameters(PreparedStatement ps, UUID profilerId, String name,
                                    java.sql.Timestamp started, double durationMilliseconds,
                                    String userName, boolean hasUserViewed,
                                    String machineName, String profileJson) throws SQLException {
        setUuid(ps, 1, profilerId);
        ps.setString(2, name);
        ps.setTimestamp(3, started);
        ps.setDouble(4, durationMilliseconds);
        ps.setString(5, userName);
        ps.setBoolean(6, hasUserViewed);
        ps.setString(7, machineName);
        ps.setString(8, profileJson);
    }

    /**
     * Returns the SELECT SQL to load a single profiler by its UUID.
     * The statement expects one parameter: the profiler_id (UUID).
     * The result set must include a column for the JSON profile data.
     *
     * @param tableName the table name
     * @return the SQL string
     */
    String getLoadSql(String tableName);

    /**
     * Returns the SELECT SQL to list profiler UUIDs within a date range.
     * Parameters are bound by {@link #bindListParameters}.
     *
     * @param tableName the table name
     * @param order     the sort order
     * @return the SQL string
     */
    String getListSql(String tableName, ListResultsOrder order);

    /**
     * Binds parameters for the list SQL returned by {@link #getListSql}.
     * The default implementation binds: 1=start, 2=finish, 3=maxResults.
     * Dialects with different parameter order (e.g. MSSQL's {@code TOP})
     * should override this method.
     *
     * @param ps         the prepared statement
     * @param start      the start timestamp
     * @param finish     the finish timestamp
     * @param maxResults the maximum number of results
     * @throws SQLException if a database access error occurs
     */
    default void bindListParameters(PreparedStatement ps, java.sql.Timestamp start,
                                    java.sql.Timestamp finish, int maxResults) throws SQLException {
        ps.setTimestamp(1, start);
        ps.setTimestamp(2, finish);
        ps.setInt(3, maxResults);
    }

    /**
     * Returns the UPDATE SQL to mark a profiler session as viewed.
     * The statement expects parameters:
     * <ol>
     *   <li>user_name (String)</li>
     *   <li>profiler_id (UUID)</li>
     * </ol>
     *
     * @param tableName the table name
     * @return the SQL string
     */
    String getSetViewedSql(String tableName);

    /**
     * Returns the UPDATE SQL to mark a profiler session as unviewed.
     * The statement expects parameters:
     * <ol>
     *   <li>user_name (String)</li>
     *   <li>profiler_id (UUID)</li>
     * </ol>
     *
     * @param tableName the table name
     * @return the SQL string
     */
    String getSetUnviewedSql(String tableName);

    /**
     * Returns the SELECT SQL to get all unviewed profiler UUIDs for a given user.
     * The statement expects one parameter: the user_name (String).
     *
     * @param tableName the table name
     * @return the SQL string
     */
    String getGetUnviewedIdsSql(String tableName);

    /**
     * Returns the DELETE SQL to remove sessions started before a given cutoff.
     * The statement expects one parameter: the cutoff timestamp (Timestamp).
     *
     * @param tableName the table name
     * @return the SQL string
     */
    String getExpireSql(String tableName);

    /**
     * Returns the SQL to remove all rows from the table.
     *
     * @param tableName the table name
     * @return the SQL string
     */
    String getClearSql(String tableName);

    /**
     * Returns the column name used for the JSON profile data in this dialect.
     *
     * @return the column name
     */
    String getJsonColumnName();

    /**
     * Returns the column name used for the profiler UUID in this dialect.
     *
     * @return the column name
     */
    String getProfilerIdColumnName();

    /**
     * Sets a UUID parameter on a prepared statement in the dialect-appropriate way.
     *
     * @param ps    the prepared statement
     * @param index the parameter index (1-based)
     * @param uuid  the UUID value
     * @throws SQLException if a database access error occurs
     */
    void setUuid(PreparedStatement ps, int index, UUID uuid) throws SQLException;

    /**
     * Reads a UUID value from a result set column in the dialect-appropriate way.
     *
     * @param rs         the result set
     * @param columnName the column name
     * @return the UUID value
     * @throws SQLException if a database access error occurs
     */
    UUID getUuid(ResultSet rs, String columnName) throws SQLException;

    /**
     * Auto-detects the appropriate dialect from a JDBC URL.
     *
     * @param jdbcUrl the JDBC URL
     * @return the detected dialect
     * @throws IllegalArgumentException if the URL does not match any known dialect
     */
    static DatabaseDialect detect(String jdbcUrl) {
        if (jdbcUrl == null) {
            throw new IllegalArgumentException("JDBC URL must not be null");
        }
        String lower = jdbcUrl.toLowerCase();
        if (lower.contains(":h2:")) {
            return new H2Dialect();
        }
        if (lower.contains(":postgresql:")) {
            return new PostgresDialect();
        }
        if (lower.contains(":mysql:") || lower.contains(":mariadb:")) {
            return new MysqlDialect();
        }
        if (lower.contains(":sqlserver:")) {
            return new MssqlDialect();
        }
        if (lower.contains(":oracle:")) {
            return new OracleDialect();
        }
        throw new IllegalArgumentException("Cannot detect database dialect from JDBC URL: " + jdbcUrl);
    }

    /**
     * Returns a dialect instance for the given name.
     *
     * @param name the dialect name: {@code h2}, {@code postgresql}, {@code mysql},
     *             {@code mssql}, or {@code oracle}
     * @return the dialect instance
     * @throws IllegalArgumentException if the name is not recognised
     */
    static DatabaseDialect forName(String name) {
        switch (name.toLowerCase()) {
            case "h2":
                return new H2Dialect();
            case "postgresql":
                return new PostgresDialect();
            case "mysql":
                return new MysqlDialect();
            case "mssql":
                return new MssqlDialect();
            case "oracle":
                return new OracleDialect();
            default:
                throw new IllegalArgumentException("Unknown dialect: " + name);
        }
    }
}
