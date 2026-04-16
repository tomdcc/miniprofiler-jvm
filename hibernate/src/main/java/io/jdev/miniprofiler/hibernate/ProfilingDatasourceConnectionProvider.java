/*
 * Copyright 2014-2026 the original author or authors.
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

package io.jdev.miniprofiler.hibernate;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.jdbc.ProfilingConnectionWrapper;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;

import java.sql.Connection;
import java.sql.SQLException;

/** A Hibernate {@link DatasourceConnectionProviderImpl} that records SQL query timings in MiniProfiler. */
public class ProfilingDatasourceConnectionProvider extends DatasourceConnectionProviderImpl {

    /** Wraps connections for profiling. */
    private final ProfilingConnectionWrapper connectionWrapper;

    /** Creates a new instance. The profiler provider is resolved via the service locator on first use. */
    public ProfilingDatasourceConnectionProvider() {
        connectionWrapper = new ProfilingConnectionWrapper();
    }

    /**
     * Creates a new instance using the given profiler provider.
     *
     * @param profilerProvider the profiler provider to use
     */
    public ProfilingDatasourceConnectionProvider(ProfilerProvider profilerProvider) {
        connectionWrapper = new ProfilingConnectionWrapper(profilerProvider);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connectionWrapper.wrap(super.getConnection());
    }

}
