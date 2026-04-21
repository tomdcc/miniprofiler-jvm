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
 *   <li>{@code storage.jdbc.jndiName} — JNDI name of a container-managed {@code DataSource};
 *       takes precedence over the URL path when set</li>
 *   <li>{@code storage.jdbc.url} — JDBC URL; presence triggers auto-configuration</li>
 *   <li>{@code storage.jdbc.username} — database username</li>
 *   <li>{@code storage.jdbc.password} — database password</li>
 *   <li>{@code storage.jdbc.table} — table name override</li>
 *   <li>{@code storage.jdbc.table.create} — when {@code true}, the locator calls
 *       {@link JdbcStorage#createTable()} after constructing the storage; defaults to
 *       {@code false}</li>
 *   <li>{@code storage.jdbc.dialect} — explicit dialect: h2, postgresql, mysql, mssql, oracle</li>
 * </ul>
 *
 * <p>Either {@code jndiName} or {@code url} must be set for {@link #isConfigured()} to return
 * {@code true}.</p>
 */
public class JdbcStorageConfig {

    private final String url;
    private final String username;
    private final String password;
    private final String table;
    private final String dialect;
    private final String jndiName;
    private final boolean tableCreate;

    /**
     * Creates a new instance with explicit values.
     *
     * @param url         the JDBC URL; may be {@code null}
     * @param username    the database username; may be {@code null}
     * @param password    the database password; may be {@code null}
     * @param table       the table name override; may be {@code null}
     * @param dialect     the dialect name override; may be {@code null}
     * @param jndiName    the JNDI name of a container-managed DataSource; may be {@code null}
     * @param tableCreate whether the locator should call {@link JdbcStorage#createTable()} after
     *                    constructing the storage
     */
    public JdbcStorageConfig(String url, String username, String password, String table, String dialect,
                             String jndiName, boolean tableCreate) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.table = table;
        this.dialect = dialect;
        this.jndiName = jndiName;
        this.tableCreate = tableCreate;
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
        String url          = props.getProperty("storage.jdbc.url",          (String) null);
        String username     = props.getProperty("storage.jdbc.username",     (String) null);
        String password     = props.getProperty("storage.jdbc.password",     (String) null);
        String table        = props.getProperty("storage.jdbc.table",        (String) null);
        String dialect      = props.getProperty("storage.jdbc.dialect",      (String) null);
        String jndiName     = props.getProperty("storage.jdbc.jndiName",     (String) null);
        boolean tableCreate = props.getProperty("storage.jdbc.table.create", false);
        return new JdbcStorageConfig(url, username, password, table, dialect, jndiName, tableCreate);
    }

    /**
     * Returns whether either a JNDI name or JDBC URL is set, indicating auto-configuration
     * should proceed.
     *
     * @return {@code true} if either {@code jndiName} or {@code url} is non-null
     */
    public boolean isConfigured() {
        return jndiName != null || url != null;
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

    /**
     * Returns the JNDI name of the {@code DataSource} to look up, or {@code null} if not set.
     *
     * @return the JNDI name
     */
    public String getJndiName() {
        return jndiName;
    }

    /**
     * Returns whether the locator should auto-create the storage table after constructing the
     * storage. Defaults to {@code false}.
     *
     * @return {@code true} if {@code storage.jdbc.table.create} is set to {@code true}
     */
    public boolean isTableCreate() {
        return tableCreate;
    }
}
