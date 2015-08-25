/*
 * Copyright 2013 the original author or authors.
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

package io.jdev.miniprofiler.sql;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.StaticProfilerProvider;
import io.jdev.miniprofiler.sql.log4jdbc.ConnectionSpy;
import io.jdev.miniprofiler.sql.log4jdbc.SpyLogFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class ProfilingDataSource implements DataSource {

    private final DataSource targetDataSource;

    public ProfilingDataSource(DataSource targetDataSource) {
        this(targetDataSource, new StaticProfilerProvider());
    }

    public ProfilingDataSource(DataSource targetDataSource, ProfilerProvider profilerProvider) {
        this.targetDataSource = targetDataSource;
        SpyLogFactory.setSpyLogDelegator(new ProfilingSpyLogDelegator(profilerProvider));
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = targetDataSource.getConnection();
        return new ConnectionSpy(conn);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = targetDataSource.getConnection(username, password);
        return new ConnectionSpy(conn);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return targetDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        targetDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        targetDataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return targetDataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return targetDataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return targetDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return targetDataSource.isWrapperFor(iface);
    }
}
