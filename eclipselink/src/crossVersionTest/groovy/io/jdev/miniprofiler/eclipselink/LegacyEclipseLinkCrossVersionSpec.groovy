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
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import io.jdev.miniprofiler.test.TestProfilerProviderLocator
import org.eclipse.persistence.sessions.DatabaseLogin
import org.eclipse.persistence.sessions.Project
import spock.lang.Requires
import spock.lang.Specification

/**
 * Tests {@link LegacyProfilingSessionCustomizer} which implements
 * {@code org.eclipse.persistence.config.SessionCustomizer} (EclipseLink 2.x/3.x).
 */
@Requires({ classExists('org.eclipse.persistence.config.SessionCustomizer') })
class LegacyEclipseLinkCrossVersionSpec extends Specification {

    TestProfilerProvider profilerProvider = new TestProfilerProvider()

    void cleanup() {
        TestProfilerProviderLocator.deactivate()
        MiniProfiler.profilerProvider = null
    }

    void "DI mode: constructor-injected provider captures SQL"() {
        given:
        def profiler = profilerProvider.start("legacy-di-test")
        def dbSession = createSession("eclipselink_legacy_di_test")

        def customizer = new LegacyProfilingSessionCustomizer(profilerProvider)
        customizer.customize(dbSession)
        dbSession.login()

        when:
        dbSession.executeSQL("SELECT 1")

        then:
        hasProfiledSql(profiler, "SELECT 1")

        cleanup:
        dbSession?.logout()
        profiler?.stop()
    }

    void "ServiceLoader mode: profiler provider discovered via locator captures SQL"() {
        given:
        TestProfilerProviderLocator.activate(profilerProvider)
        def profiler = profilerProvider.start("legacy-serviceloader-test")
        def dbSession = createSession("eclipselink_legacy_sl_test")

        def customizer = new LegacyProfilingSessionCustomizer()
        customizer.customize(dbSession)
        dbSession.login()

        when:
        dbSession.executeSQL("SELECT 1")

        then:
        hasProfiledSql(profiler, "SELECT 1")

        cleanup:
        dbSession?.logout()
        profiler?.stop()
    }

    void "Static fallback mode: falls through to MiniProfiler static provider"() {
        given:
        TestProfilerProviderLocator.deactivate()
        MiniProfiler.profilerProvider = profilerProvider
        def profiler = profilerProvider.start("legacy-static-test")
        def dbSession = createSession("eclipselink_legacy_static_test")

        def customizer = new LegacyProfilingSessionCustomizer()
        customizer.customize(dbSession)
        dbSession.login()

        when:
        dbSession.executeSQL("SELECT 1")

        then:
        hasProfiledSql(profiler, "SELECT 1")

        cleanup:
        dbSession?.logout()
        profiler?.stop()
    }

    private static createSession(String dbName) {
        def login = new DatabaseLogin()
        login.setDriverClassName("org.h2.Driver")
        login.setConnectionString("jdbc:h2:mem:${dbName};DB_CLOSE_DELAY=-1")
        new Project(login).createDatabaseSession()
    }

    private static boolean hasProfiledSql(Profiler profiler, String expectedFragment) {
        def sqlTimings = profiler.root.customTimings["sql"]
        return sqlTimings != null && sqlTimings.any { it.commandString.contains(expectedFragment) }
    }

    private static boolean classExists(String name) {
        try { Class.forName(name); true } catch (ClassNotFoundException e) { false }
    }
}
