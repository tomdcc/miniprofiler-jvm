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

package io.jdev.miniprofiler.jdbc;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerProviderLocator;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.proxy.ProxyConfig;

import java.sql.Connection;

/**
 * Wraps individual {@link Connection} instances so that SQL queries executed
 * through them are recorded in MiniProfiler.
 *
 * <p>This is intended for integration points (e.g. Hibernate, EclipseLink)
 * that hand out connections one at a time rather than through a
 * {@link javax.sql.DataSource}. For the {@code DataSource} case, prefer
 * {@link ProfilingDataSource}.</p>
 *
 * <p>Two construction modes are supported:</p>
 * <ul>
 *   <li>{@link #ProfilingConnectionWrapper(ProfilerProvider)} &mdash; eagerly binds to the given provider.</li>
 *   <li>{@link #ProfilingConnectionWrapper()} &mdash; defers provider resolution to the first call to
 *       {@link #wrap(Connection)}, using {@link ProfilerProviderLocator#findProvider()}.</li>
 * </ul>
 */
public class ProfilingConnectionWrapper {

    private volatile ProxyConfig proxyConfig;

    /**
     * Creates a wrapper that resolves the {@link ProfilerProvider} lazily via
     * {@link ProfilerProviderLocator#findProvider()} on first use.
     */
    public ProfilingConnectionWrapper() {
    }

    /**
     * Creates a wrapper bound to the given profiler provider.
     *
     * @param profilerProvider the provider used to record query timings
     */
    public ProfilingConnectionWrapper(ProfilerProvider profilerProvider) {
        this.proxyConfig = buildConfig(profilerProvider);
    }

    /**
     * Returns a profiling-aware proxy around the given connection.
     *
     * @param connection the connection to wrap
     * @return a proxied connection that records SQL timings in MiniProfiler
     */
    public Connection wrap(Connection connection) {
        ProxyConfig config = ensureConfig();
        return config.getJdbcProxyFactory()
            .createConnection(connection, new ConnectionInfo(), config);
    }

    private ProxyConfig ensureConfig() {
        ProxyConfig config = proxyConfig;
        if (config == null) {
            synchronized (this) {
                config = proxyConfig;
                if (config == null) {
                    config = buildConfig(ProfilerProviderLocator.findProvider());
                    proxyConfig = config;
                }
            }
        }
        return config;
    }

    private static ProxyConfig buildConfig(ProfilerProvider profilerProvider) {
        return new ProxyConfig.Builder()
            .queryListener(new ProfilingQueryExecutionListener(profilerProvider))
            .build();
    }
}
