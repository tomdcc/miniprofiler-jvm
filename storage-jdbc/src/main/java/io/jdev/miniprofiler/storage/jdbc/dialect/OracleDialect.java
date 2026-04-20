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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Oracle database dialect. Uses {@code VARCHAR2(36)} for UUIDs,
 * {@code TIMESTAMP(6)} for timestamps, {@code NUMBER(1)} for booleans,
 * and {@code CLOB} for JSON. Requires Oracle 12c or later for
 * {@code FETCH FIRST N ROWS ONLY} syntax.
 */
public class OracleDialect implements DatabaseDialect {

    /** Creates a new instance. */
    public OracleDialect() {
    }

    /** ORA-00955: name is already used by an existing object. */
    private static final int ORA_NAME_ALREADY_USED = 955;

    @Override
    public String getCreateTableDdl(String tableName) {
        return "CREATE TABLE " + tableName + " ("
            + "id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "profiler_id VARCHAR2(36) NOT NULL, "
            + "name VARCHAR2(200), "
            + "started TIMESTAMP(6) NOT NULL, "
            + "duration_milliseconds NUMBER(18,3), "
            + "user_name VARCHAR2(100), "
            + "has_user_viewed NUMBER(1) DEFAULT 0 NOT NULL, "
            + "machine_name VARCHAR2(100), "
            + "profile_json CLOB"
            + ");\n"
            + "CREATE UNIQUE INDEX idx_" + tableName + "_profiler_id ON " + tableName + " (profiler_id);\n"
            + "CREATE INDEX idx_" + tableName + "_user_viewed ON " + tableName + " (user_name, has_user_viewed);\n"
            + "CREATE INDEX idx_" + tableName + "_started ON " + tableName + " (started);";
    }

    @Override
    public void executeCreateTable(Connection conn, String tableName) throws SQLException {
        String ddl = getCreateTableDdl(tableName);
        for (String statement : ddl.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                try (Statement st = conn.createStatement()) {
                    st.execute(trimmed);
                } catch (SQLException e) {
                    if (e.getErrorCode() != ORA_NAME_ALREADY_USED) {
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public String getSaveSql(String tableName) {
        return "MERGE INTO " + tableName + " t"
            + " USING (SELECT ? AS profiler_id, ? AS name, ? AS started,"
            + " ? AS duration_milliseconds, ? AS user_name, ? AS has_user_viewed,"
            + " ? AS machine_name, ? AS profile_json FROM DUAL) s"
            + " ON (t.profiler_id = s.profiler_id)"
            + " WHEN MATCHED THEN UPDATE SET"
            + " t.name = s.name,"
            + " t.duration_milliseconds = s.duration_milliseconds,"
            + " t.machine_name = s.machine_name,"
            + " t.profile_json = s.profile_json"
            + " WHEN NOT MATCHED THEN INSERT"
            + " (profiler_id, name, started, duration_milliseconds, user_name, has_user_viewed, machine_name, profile_json)"
            + " VALUES (s.profiler_id, s.name, s.started, s.duration_milliseconds,"
            + " s.user_name, s.has_user_viewed, s.machine_name, s.profile_json)";
    }

    @Override
    public void bindSaveParameters(PreparedStatement ps, UUID profilerId, String name,
                                   Timestamp started, double durationMilliseconds,
                                   String userName, boolean hasUserViewed,
                                   String machineName, String profileJson) throws SQLException {
        setUuid(ps, 1, profilerId);
        ps.setString(2, name);
        ps.setTimestamp(3, started);
        ps.setDouble(4, durationMilliseconds);
        ps.setString(5, userName);
        ps.setInt(6, hasUserViewed ? 1 : 0);
        ps.setString(7, machineName);
        ps.setString(8, profileJson);
    }

    @Override
    public String getLoadSql(String tableName) {
        return "SELECT profile_json FROM " + tableName + " WHERE profiler_id = ?";
    }

    @Override
    public String getListSql(String tableName, ListResultsOrder order) {
        String dir = order == ListResultsOrder.Descending ? "DESC" : "ASC";
        return "SELECT profiler_id FROM " + tableName
            + " WHERE started >= ? AND started <= ?"
            + " ORDER BY started " + dir
            + " FETCH FIRST ? ROWS ONLY";
    }

    @Override
    public String getSetViewedSql(String tableName) {
        return "UPDATE " + tableName + " SET has_user_viewed = 1 WHERE user_name = ? AND profiler_id = ?";
    }

    @Override
    public String getSetUnviewedSql(String tableName) {
        return "UPDATE " + tableName + " SET has_user_viewed = 0 WHERE user_name = ? AND profiler_id = ?";
    }

    @Override
    public String getGetUnviewedIdsSql(String tableName) {
        return "SELECT profiler_id FROM " + tableName + " WHERE user_name = ? AND has_user_viewed = 0";
    }

    @Override
    public String getExpireSql(String tableName) {
        return "DELETE FROM " + tableName + " WHERE started < ?";
    }

    @Override
    public String getClearSql(String tableName) {
        return "DELETE FROM " + tableName;
    }

    @Override
    public String getJsonColumnName() {
        return "profile_json";
    }

    @Override
    public String getProfilerIdColumnName() {
        return "profiler_id";
    }

    @Override
    public void setUuid(PreparedStatement ps, int index, UUID uuid) throws SQLException {
        ps.setString(index, uuid.toString());
    }

    @Override
    public UUID getUuid(ResultSet rs, String columnName) throws SQLException {
        return UUID.fromString(rs.getString(columnName));
    }
}
