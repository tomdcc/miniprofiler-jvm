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

import io.jdev.miniprofiler.MiniProfilerConfig;

import java.util.Properties;

/**
 * Configuration for the JDBC storage backend.
 *
 * <p>Properties are read via {@link MiniProfilerConfig}: system properties
 * (prefix {@code miniprofiler.}) take precedence over {@code miniprofiler.properties}
 * on the classpath.</p>
 *
 * <p>Supported keys:</p>
 * <ul>
 *   <li>{@code storage.jdbc.url} — JDBC URL; presence triggers auto-configuration</li>
 *   <li>{@code storage.jdbc.username} — database username</li>
 *   <li>{@code storage.jdbc.password} — database password</li>
 *   <li>{@code storage.jdbc.table} — table name override</li>
 *   <li>{@code storage.jdbc.dialect} — explicit dialect: h2, postgresql, mysql, mssql, oracle</li>
 * </ul>
 */
public class JdbcStorageConfig {

    private final String url;
    private final String username;
    private final String password;
    private final String table;
    private final String dialect;

    /**
     * Creates a new instance with explicit values.
     *
     * @param url      the JDBC URL; may be {@code null}
     * @param username the database username; may be {@code null}
     * @param password the database password; may be {@code null}
     * @param table    the table name override; may be {@code null}
     * @param dialect  the dialect name override; may be {@code null}
     */
    public JdbcStorageConfig(String url, String username, String password, String table, String dialect) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.table = table;
        this.dialect = dialect;
    }

    /**
     * Creates a new {@link JdbcStorageConfig} from system properties and
     * {@code miniprofiler.properties}, falling back to {@code null} for each unset property.
     *
     * @return a new config populated from the environment
     */
    public static JdbcStorageConfig create() {
        return create(new MiniProfilerConfig());
    }

    static JdbcStorageConfig create(Properties systemProps, Properties fileProps) {
        return create(new MiniProfilerConfig(systemProps, fileProps));
    }

    static JdbcStorageConfig create(MiniProfilerConfig props) {
        String url      = props.getProperty("storage.jdbc.url",      (String) null);
        String username = props.getProperty("storage.jdbc.username", (String) null);
        String password = props.getProperty("storage.jdbc.password", (String) null);
        String table    = props.getProperty("storage.jdbc.table",    (String) null);
        String dialect  = props.getProperty("storage.jdbc.dialect",  (String) null);
        return new JdbcStorageConfig(url, username, password, table, dialect);
    }

    /**
     * Returns whether the JDBC URL is set, indicating auto-configuration should proceed.
     *
     * @return {@code true} if the JDBC URL is non-null
     */
    public boolean isConfigured() {
        return url != null;
    }

    /**
     * Returns the JDBC URL, or {@code null} if not set.
     *
     * @return the JDBC URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the database username, or {@code null} if not set.
     *
     * @return the database username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the database password, or {@code null} if not set.
     *
     * @return the database password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the table name override, or {@code null} if not set.
     *
     * @return the table name
     */
    public String getTable() {
        return table;
    }

    /**
     * Returns the dialect name override, or {@code null} if not set.
     *
     * @return the dialect name
     */
    public String getDialect() {
        return dialect;
    }
}
