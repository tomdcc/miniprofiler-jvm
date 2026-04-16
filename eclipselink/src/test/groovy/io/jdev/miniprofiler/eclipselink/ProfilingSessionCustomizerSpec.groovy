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

package io.jdev.miniprofiler.eclipselink

import io.jdev.miniprofiler.MiniProfiler
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.test.TestProfilerProvider
import org.eclipse.persistence.sessions.Connector
import org.eclipse.persistence.sessions.DatabaseLogin
import org.eclipse.persistence.sessions.DatasourceLogin
import org.eclipse.persistence.sessions.Session
import spock.lang.Specification

class ProfilingSessionCustomizerSpec extends Specification {

    Session session = Mock()
    DatabaseLogin login = Mock()
    Connector originalConnector = Mock()

    void setup() {
        session.getLogin() >> login
        login.getConnector() >> originalConnector
    }

    void cleanup() {
        MiniProfiler.profilerProvider = null
    }

    void "constructor with provider stores it for use in customize"() {
        given:
        def profilerProvider = new TestProfilerProvider()
        def customizer = new ProfilingSessionCustomizer(profilerProvider)

        when:
        customizer.customize(session)

        then:
        1 * login.setConnector({ it instanceof ProfilingConnector && it.@target == originalConnector })
    }

    void "no-arg constructor uses ProfilerProviderLocator in customize"() {
        given:
        def testProvider = new TestProfilerProvider()
        MiniProfiler.profilerProvider = testProvider
        def customizer = new ProfilingSessionCustomizer()

        when:
        customizer.customize(session)

        then:
        1 * login.setConnector({ it instanceof ProfilingConnector && it.@target == originalConnector })
    }

    void "customize wraps the session login connector with a ProfilingConnector"() {
        given:
        def customizer = new ProfilingSessionCustomizer(Mock(ProfilerProvider))

        when:
        customizer.customize(session)

        then:
        1 * login.setConnector({ it instanceof ProfilingConnector && it.@target == originalConnector })
    }

    void "customize wraps the read pool connector on a ServerSession"() {
        given:
        def readPoolLogin = Mock(DatasourceLogin)
        def readConnector = Mock(Connector)
        readPoolLogin.getConnector() >> readConnector

        def readPool = Mock(org.eclipse.persistence.sessions.server.ConnectionPool)
        readPool.getLogin() >> readPoolLogin

        def serverSession = Mock(org.eclipse.persistence.sessions.server.ServerSession)
        serverSession.getLogin() >> login
        login.getConnector() >> originalConnector
        serverSession.getReadConnectionPool() >> readPool

        def customizer = new ProfilingSessionCustomizer(Mock(ProfilerProvider))

        when:
        customizer.customize(serverSession)

        then:
        1 * login.setConnector({ it instanceof ProfilingConnector })
        1 * readPoolLogin.setConnector({ it instanceof ProfilingConnector && it.@target == readConnector })
    }
}
