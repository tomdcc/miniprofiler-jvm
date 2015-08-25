/*
 * Copyright 2014 the original author or authors.
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

package io.jdev.miniprofiler.sql.hibernate;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.StaticProfilerProvider;
import io.jdev.miniprofiler.sql.ProfilingSpyLogDelegator;
import io.jdev.miniprofiler.sql.log4jdbc.ConnectionSpy;
import io.jdev.miniprofiler.sql.log4jdbc.SpyLogFactory;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;

import java.sql.Connection;
import java.sql.SQLException;

public class ProfilingDatasourceConnectionProvider extends DatasourceConnectionProviderImpl {

    private final ProfilerProvider profilerProvider;

    public ProfilingDatasourceConnectionProvider() {
        this(new StaticProfilerProvider());
    }

    public ProfilingDatasourceConnectionProvider(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
        SpyLogFactory.setSpyLogDelegator(new ProfilingSpyLogDelegator(profilerProvider));
    }

    @Override
    public Connection getConnection() throws SQLException {
        return new ConnectionSpy(super.getConnection());
    }

}
