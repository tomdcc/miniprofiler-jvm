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

package io.jdev.miniprofiler.jdbc;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerProviderLocator;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * A {@link DataSource} wrapper that records SQL query timings in MiniProfiler using
 * <a href="https://github.com/jdbc-observations/datasource-proxy">datasource-proxy</a>.
 *
 * <p>Each instance wraps the target data source with a private
 * {@link net.ttddyy.dsproxy.support.ProxyDataSource} and registers a
 * {@link ProfilingQueryExecutionListener} tied to the supplied
 * {@link ProfilerProvider}. There is no shared or static state, so multiple
 * {@code ProfilingDataSource} instances with different providers may coexist.</p>
 */
public class ProfilingDataSource implements DataSource, Closeable {

    private final DataSource delegate;
    private final ProxyDataSource proxy;

    /**
     * Creates a new instance resolving the {@link ProfilerProvider} via
     * {@link ProfilerProviderLocator#findProvider()}.
     *
     * @param delegate the data source to wrap
     */
    public ProfilingDataSource(DataSource delegate) {
        this(delegate, ProfilerProviderLocator.findProvider());
    }

    /**
     * Creates a new instance using the given profiler provider.
     *
     * @param delegate the data source to wrap
     * @param profilerProvider the profiler provider used to record query timings
     */
    public ProfilingDataSource(DataSource delegate, ProfilerProvider profilerProvider) {
        this.delegate = delegate;
        this.proxy = ProxyDataSourceBuilder.create(delegate)
            .listener(new ProfilingQueryExecutionListener(profilerProvider))
            .build();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return proxy.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return proxy.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return (T) this;
        } else if (iface.isAssignableFrom(delegate.getClass())) {
            return (T) delegate;
        } else {
            return delegate.unwrap(iface);
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(getClass())
            || iface.isAssignableFrom(delegate.getClass())
            || delegate.isWrapperFor(iface);
    }

    @Override
    public void close() throws IOException {
        if (delegate instanceof Closeable) {
            ((Closeable) delegate).close();
        } else {
            try {
                Method closeMethod = delegate.getClass().getMethod("close");
                closeMethod.invoke(delegate);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException("Data source has no close method", e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException("Error closing data source", cause);
                }
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException("Error closing data source", e);
            }
        }
    }
}
