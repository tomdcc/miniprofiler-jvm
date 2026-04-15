/*
 * Copyright 2013-2026 the original author or authors.
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

package io.jdev.miniprofiler.ratpack;

import io.jdev.miniprofiler.sql.ProfilingDataSource;
import ratpack.h2.H2Module;

import javax.sql.DataSource;

/**
 * A module that extends Ratpack's <code>H2Module</code> but provides a <code>DataSource</code>
 * that is actually a {@link ProfilingDataSource} so that JDBC calls are profiled.
 */
public class MiniProfilerH2Module extends H2Module {

    private final boolean suppressProfiling;

    /**
     * Creates a module with profiling turned off completely, mainly for testing.
     * @param suppressProfiling whether to suppress profiling
     */
    public MiniProfilerH2Module(boolean suppressProfiling) {
        this.suppressProfiling = suppressProfiling;
    }

    /** Creates a module with default settings and profiling enabled. */
    public MiniProfilerH2Module() {
        this(false);
    }

    /**
     * Creates a module with the given H2 credentials and URL, and profiling enabled.
     *
     * @param username the H2 username
     * @param password the H2 password
     * @param url      the H2 JDBC URL
     */
    public MiniProfilerH2Module(String username, String password, String url) {
        this(username, password, url, false);
    }

    /**
     * Creates a module with the given H2 credentials, URL, and suppress-profiling flag.
     *
     * @param username          the H2 username
     * @param password          the H2 password
     * @param url               the H2 JDBC URL
     * @param suppressProfiling whether to suppress profiling
     */
    public MiniProfilerH2Module(String username, String password, String url, boolean suppressProfiling) {
        super(username, password, url);
        this.suppressProfiling = suppressProfiling;
    }

    /**
     * Returns a {@link ProfilingDataSource} wrapping the H2 data source, unless profiling is suppressed.
     *
     * @return the data source, optionally wrapped for profiling
     */
    protected DataSource createDataSource() {
        DataSource ds = super.createDataSource();
        return suppressProfiling ? ds : new ProfilingDataSource(ds);
    }

}
