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

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.jdbc.ProfilingConnectionWrapper;
import org.eclipse.persistence.sessions.DatasourceLogin;
import org.eclipse.persistence.sessions.Login;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.server.ConnectionPool;
import org.eclipse.persistence.sessions.server.ServerSession;

/**
 * Shared session customization logic used by both {@link ProfilingSessionCustomizer}
 * and {@link LegacyProfilingSessionCustomizer}.
 */
class SessionCustomizerSupport {

    static void customize(ProfilerProvider explicitProvider, Session session) {
        ProfilingConnectionWrapper connectionWrapper = explicitProvider != null
            ? new ProfilingConnectionWrapper(explicitProvider)
            : new ProfilingConnectionWrapper();

        DatasourceLogin login = session.getLogin();
        login.setConnector(new ProfilingConnector(login.getConnector(), connectionWrapper));
        if (session instanceof ServerSession) {
            ServerSession serverSession = (ServerSession) session;
            ConnectionPool pool = serverSession.getReadConnectionPool();
            if (pool != null) {
                Login poolLogin = pool.getLogin();
                if (poolLogin instanceof DatasourceLogin) {
                    login = (DatasourceLogin) poolLogin;
                    login.setConnector(new ProfilingConnector(login.getConnector(), connectionWrapper));
                }
            }
        }
    }
}
