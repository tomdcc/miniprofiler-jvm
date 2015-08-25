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

package io.jdev.miniprofiler.sql.eclipselink;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.StaticProfilerProvider;
import io.jdev.miniprofiler.sql.ProfilingSpyLogDelegator;
import io.jdev.miniprofiler.sql.log4jdbc.SpyLogFactory;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.sessions.DatasourceLogin;
import org.eclipse.persistence.sessions.Login;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.server.ConnectionPool;
import org.eclipse.persistence.sessions.server.ServerSession;

/**
 * An EclipseLink SessionCustomizer that installs a
 * {@link ProfilingConnector} onto EclipseLink's read and write pools.
 *
 * <p>The customizer can be installed by setting the
 * <code>eclipselink.session.customizer</code> property to this class name
 * <code>io.jdev.miniprofiler.sql.eclipselink.ProfilingSessionCustomizer</code>
 * </p>
 */
public class ProfilingSessionCustomizer implements SessionCustomizer {

    public ProfilingSessionCustomizer() {
        this(new StaticProfilerProvider());
    }

    public ProfilingSessionCustomizer(ProfilerProvider profilerProvider) {
        SpyLogFactory.setSpyLogDelegator(new ProfilingSpyLogDelegator(profilerProvider));
    }

    @Override
    public void customize(Session session) throws Exception {
        DatasourceLogin login = session.getLogin();
        login.setConnector(new ProfilingConnector(login.getConnector()));
        if (session instanceof ServerSession) {
            ServerSession serverSession = (ServerSession) session;
            ConnectionPool pool = serverSession.getReadConnectionPool();
            if (pool != null) {
                Login poolLogin = pool.getLogin();
                if (poolLogin instanceof DatasourceLogin) {
                    login = (DatasourceLogin) poolLogin;
                    login.setConnector(new ProfilingConnector(login.getConnector()));
                }
            }
        }
    }
}
