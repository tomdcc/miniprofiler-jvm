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
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Microsoft SQL Server dialect. Uses PascalCase column names,
 * {@code uniqueidentifier} for UUIDs, {@code datetime2(6)} for timestamps,
 * {@code bit} for booleans, and {@code nvarchar(max)} for JSON.
 */
public class MssqlDialect implements DatabaseDialect {

    /** Creates a new instance. */
    public MssqlDialect() {
    }

    @Override
    public String getCreateTableDdl(String tableName) {
        return "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = '" + tableName + "')\n"
            + "CREATE TABLE [" + tableName + "] ("
            + "[Id] BIGINT IDENTITY PRIMARY KEY, "
            + "[ProfilerId] uniqueidentifier NOT NULL, "
            + "[Name] NVARCHAR(200), "
            + "[Started] datetime2(6) NOT NULL, "
            + "[DurationMilliseconds] DECIMAL(18,3), "
            + "[UserName] NVARCHAR(100), "
            + "[HasUserViewed] BIT NOT NULL DEFAULT 0, "
            + "[MachineName] NVARCHAR(100), "
            + "[ProfileJson] NVARCHAR(MAX)"
            + ");\n"
            + "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_" + tableName + "_ProfilerId')\n"
            + "CREATE UNIQUE INDEX [idx_" + tableName + "_ProfilerId] ON [" + tableName + "] ([ProfilerId]);\n"
            + "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_" + tableName + "_UserViewed')\n"
            + "CREATE INDEX [idx_" + tableName + "_UserViewed] ON [" + tableName + "] ([UserName], [HasUserViewed]);\n"
            + "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_" + tableName + "_Started')\n"
            + "CREATE INDEX [idx_" + tableName + "_Started] ON [" + tableName + "] ([Started]);";
    }

    @Override
    public String getSaveSql(String tableName) {
        return "MERGE [" + tableName + "] WITH (HOLDLOCK) AS target"
            + " USING (SELECT ? AS [ProfilerId], ? AS [Name], ? AS [Started],"
            + " ? AS [DurationMilliseconds], ? AS [UserName], ? AS [HasUserViewed],"
            + " ? AS [MachineName], ? AS [ProfileJson]) AS source"
            + " ON target.[ProfilerId] = source.[ProfilerId]"
            + " WHEN MATCHED THEN UPDATE SET"
            + " target.[Name] = source.[Name],"
            + " target.[DurationMilliseconds] = source.[DurationMilliseconds],"
            + " target.[MachineName] = source.[MachineName],"
            + " target.[ProfileJson] = source.[ProfileJson]"
            + " WHEN NOT MATCHED THEN INSERT"
            + " ([ProfilerId], [Name], [Started], [DurationMilliseconds],"
            + " [UserName], [HasUserViewed], [MachineName], [ProfileJson])"
            + " VALUES (source.[ProfilerId], source.[Name], source.[Started],"
            + " source.[DurationMilliseconds], source.[UserName], source.[HasUserViewed],"
            + " source.[MachineName], source.[ProfileJson]);";
    }

    @Override
    public String getLoadSql(String tableName) {
        return "SELECT [ProfileJson] FROM [" + tableName + "] WHERE [ProfilerId] = ?";
    }

    @Override
    public String getListSql(String tableName, ListResultsOrder order) {
        String dir = order == ListResultsOrder.Descending ? "DESC" : "ASC";
        return "SELECT TOP (?) [ProfilerId] FROM [" + tableName + "]"
            + " WHERE [Started] >= ? AND [Started] <= ?"
            + " ORDER BY [Started] " + dir;
    }

    @Override
    public void bindListParameters(PreparedStatement ps, Timestamp start,
                                   Timestamp finish, int maxResults) throws SQLException {
        ps.setInt(1, maxResults);
        ps.setTimestamp(2, start);
        ps.setTimestamp(3, finish);
    }

    @Override
    public String getSetViewedSql(String tableName) {
        return "UPDATE [" + tableName + "] SET [HasUserViewed] = 1 WHERE [UserName] = ? AND [ProfilerId] = ?";
    }

    @Override
    public String getSetUnviewedSql(String tableName) {
        return "UPDATE [" + tableName + "] SET [HasUserViewed] = 0 WHERE [UserName] = ? AND [ProfilerId] = ?";
    }

    @Override
    public String getGetUnviewedIdsSql(String tableName) {
        return "SELECT [ProfilerId] FROM [" + tableName + "] WHERE [UserName] = ? AND [HasUserViewed] = 0";
    }

    @Override
    public String getExpireSql(String tableName) {
        return "DELETE FROM [" + tableName + "] WHERE [Started] < ?";
    }

    @Override
    public String getClearSql(String tableName) {
        return "DELETE FROM [" + tableName + "]";
    }

    @Override
    public String getJsonColumnName() {
        return "ProfileJson";
    }

    @Override
    public String getProfilerIdColumnName() {
        return "ProfilerId";
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
