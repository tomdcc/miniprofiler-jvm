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
 * PostgreSQL database dialect. Uses native {@code uuid} type, {@code timestamp(3)},
 * {@code boolean}, and {@code text}.
 */
public class PostgresDialect implements DatabaseDialect {

    /** Creates a new instance. */
    public PostgresDialect() {
    }

    @Override
    public String getCreateTableDdl(String tableName) {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
            + "id BIGSERIAL PRIMARY KEY, "
            + "profiler_id uuid NOT NULL, "
            + "name VARCHAR(200), "
            + "started TIMESTAMP(6) NOT NULL, "
            + "duration_milliseconds DECIMAL(18,3), "
            + "user_name VARCHAR(100), "
            + "has_user_viewed BOOLEAN NOT NULL DEFAULT FALSE, "
            + "machine_name VARCHAR(100), "
            + "profile_json TEXT"
            + ");\n"
            + "CREATE UNIQUE INDEX IF NOT EXISTS idx_" + tableName + "_profiler_id ON " + tableName + " (profiler_id);\n"
            + "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_user_viewed ON " + tableName + " (user_name, has_user_viewed);\n"
            + "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_started ON " + tableName + " (started);";
    }

    @Override
    public String getSaveSql(String tableName) {
        return "INSERT INTO " + tableName
            + " (profiler_id, name, started, duration_milliseconds, user_name, has_user_viewed, machine_name, profile_json)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            + " ON CONFLICT (profiler_id) DO UPDATE SET"
            + " name = EXCLUDED.name,"
            + " duration_milliseconds = EXCLUDED.duration_milliseconds,"
            + " machine_name = EXCLUDED.machine_name,"
            + " profile_json = EXCLUDED.profile_json";
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
            + " LIMIT ?";
    }

    @Override
    public String getSetViewedSql(String tableName) {
        return "UPDATE " + tableName + " SET has_user_viewed = TRUE WHERE user_name = ? AND profiler_id = ?";
    }

    @Override
    public String getSetUnviewedSql(String tableName) {
        return "UPDATE " + tableName + " SET has_user_viewed = FALSE WHERE user_name = ? AND profiler_id = ?";
    }

    @Override
    public String getGetUnviewedIdsSql(String tableName) {
        return "SELECT profiler_id FROM " + tableName + " WHERE user_name = ? AND has_user_viewed = FALSE";
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
        ps.setObject(index, uuid);
    }

    @Override
    public UUID getUuid(ResultSet rs, String columnName) throws SQLException {
        return (UUID) rs.getObject(columnName);
    }
}
