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
     *
     * @param tableName the table name
     * @return the DDL string (may contain multiple statements separated by semicolons)
     */
    String getCreateTableDdl(String tableName);

    /**
     * Returns the INSERT SQL that avoids duplicates on the profiler_id column.
     * The statement expects parameters in this order:
     * <ol>
     *   <li>profiler_id (UUID)</li>
     *   <li>name (String)</li>
     *   <li>started (Timestamp)</li>
     *   <li>duration_milliseconds (double)</li>
     *   <li>user_name (String)</li>
     *   <li>has_user_viewed (boolean)</li>
     *   <li>machine_name (String)</li>
     *   <li>profile_json (String)</li>
     * </ol>
     *
     * @param tableName the table name
     * @return the SQL string
     */
    String getSaveSql(String tableName);

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
     * The statement expects parameters:
     * <ol>
     *   <li>start date (Timestamp)</li>
     *   <li>finish date (Timestamp)</li>
     *   <li>maxResults (int)</li>
     * </ol>
     *
     * @param tableName the table name
     * @param order     the sort order
     * @return the SQL string
     */
    String getListSql(String tableName, ListResultsOrder order);

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
            default:
                throw new IllegalArgumentException("Unknown dialect: " + name);
        }
    }
}
