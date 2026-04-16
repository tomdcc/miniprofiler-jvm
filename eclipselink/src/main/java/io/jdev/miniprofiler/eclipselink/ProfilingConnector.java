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

package io.jdev.miniprofiler.eclipselink;

import io.jdev.miniprofiler.jdbc.ProfilingConnectionWrapper;
import org.eclipse.persistence.sessions.Connector;
import org.eclipse.persistence.sessions.Session;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Properties;

/**
 * EclipseLink Connector implementation which returns Connection objects
 * that will profile JDBC queries via datasource-proxy.
 */
public class ProfilingConnector implements Connector {

    /** The underlying connector being wrapped. */
    private final Connector target;

    /** Wraps connections for profiling. */
    private final ProfilingConnectionWrapper connectionWrapper;

    /**
     * Creates a new connector wrapping the given target.
     *
     * @param target the connector to wrap
     * @param connectionWrapper the wrapper used to add profiling to connections
     */
    public ProfilingConnector(Connector target, ProfilingConnectionWrapper connectionWrapper) {
        this.target = target;
        this.connectionWrapper = connectionWrapper;
    }

    @Override
    public Connection connect(Properties properties, Session session) {
        return connectionWrapper.wrap(target.connect(properties, session));
    }

    @Override
    public void toString(PrintWriter writer) {
        target.toString(writer);
    }

    @Override
    public String getConnectionDetails() {
        return target.getConnectionDetails();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (Exception exception) {
            throw new InternalError("Clone failed");
        }
    }
}
