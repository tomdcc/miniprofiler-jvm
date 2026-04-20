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
 * MySQL database dialect. Uses {@code CHAR(36)} for UUIDs, {@code DATETIME(6)}
 * for timestamps, {@code BOOL}, and {@code LONGTEXT} for JSON. Tables are
 * created with InnoDB engine and {@code utf8mb4} character set.
 */
public class MysqlDialect implements DatabaseDialect {

    /** Creates a new instance. */
    public MysqlDialect() {
    }

    @Override
    public String getCreateTableDdl(String tableName) {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
            + "profiler_id CHAR(36) NOT NULL, "
            + "name VARCHAR(200), "
            + "started DATETIME(6) NOT NULL, "
            + "duration_milliseconds DECIMAL(18,3), "
            + "user_name VARCHAR(100), "
            + "has_user_viewed BOOL NOT NULL DEFAULT FALSE, "
            + "machine_name VARCHAR(100), "
            + "profile_json LONGTEXT"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n"
            + "CREATE UNIQUE INDEX idx_" + tableName + "_profiler_id ON " + tableName + " (profiler_id);\n"
            + "CREATE INDEX idx_" + tableName + "_user_viewed ON " + tableName + " (user_name, has_user_viewed);\n"
            + "CREATE INDEX idx_" + tableName + "_started ON " + tableName + " (started);";
    }

    @Override
    public String getSaveSql(String tableName) {
        return "INSERT IGNORE INTO " + tableName
            + " (profiler_id, name, started, duration_milliseconds, user_name, has_user_viewed, machine_name, profile_json)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
        ps.setString(index, uuid.toString());
    }

    @Override
    public UUID getUuid(ResultSet rs, String columnName) throws SQLException {
        return UUID.fromString(rs.getString(columnName));
    }
}
