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

package io.jdev.miniprofiler.hibernate

import io.jdev.miniprofiler.MiniProfiler
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import io.jdev.miniprofiler.test.TestProfilerProviderLocator
import org.h2.jdbcx.JdbcDataSource
import spock.lang.Specification

class HibernateCrossVersionSpec extends Specification {

    TestProfilerProvider profilerProvider = new TestProfilerProvider()

    void cleanup() {
        TestProfilerProviderLocator.deactivate()
        MiniProfiler.profilerProvider = null
    }

    void "DI mode: constructor-injected provider captures SQL"() {
        given:
        def profiler = profilerProvider.start("di-test")

        def ds = new JdbcDataSource()
        ds.setURL("jdbc:h2:mem:hibernate_di_test;DB_CLOSE_DELAY=-1")

        def connectionProvider = new ProfilingDatasourceConnectionProvider(profilerProvider)
        connectionProvider.configure(["hibernate.connection.datasource": ds])

        when:
        def conn = connectionProvider.getConnection()
        conn.createStatement().execute("SELECT 1")
        conn.close()

        then:
        hasProfiledSql(profiler, "SELECT 1")

        cleanup:
        profiler?.stop()
    }

    void "ServiceLoader mode: profiler provider discovered via locator captures SQL"() {
        given:
        TestProfilerProviderLocator.activate(profilerProvider)
        def profiler = profilerProvider.start("serviceloader-test")

        def ds = new JdbcDataSource()
        ds.setURL("jdbc:h2:mem:hibernate_sl_test;DB_CLOSE_DELAY=-1")

        def cfg = new org.hibernate.cfg.Configuration()
        cfg.getProperties().put("hibernate.connection.datasource", ds)
        cfg.setProperty("hibernate.connection.provider_class",
            "io.jdev.miniprofiler.hibernate.ProfilingDatasourceConnectionProvider")

        when:
        def sf = cfg.buildSessionFactory()
        def session = sf.openSession()
        session.doWork { conn -> conn.createStatement().execute("SELECT 1") }
        session.close()
        sf.close()

        then:
        hasProfiledSql(profiler, "SELECT 1")

        cleanup:
        profiler?.stop()
    }

    void "Static fallback mode: falls through to MiniProfiler static provider"() {
        given:
        TestProfilerProviderLocator.deactivate()
        MiniProfiler.profilerProvider = profilerProvider
        def profiler = profilerProvider.start("static-test")

        def ds = new JdbcDataSource()
        ds.setURL("jdbc:h2:mem:hibernate_static_test;DB_CLOSE_DELAY=-1")

        def cfg = new org.hibernate.cfg.Configuration()
        cfg.getProperties().put("hibernate.connection.datasource", ds)
        cfg.setProperty("hibernate.connection.provider_class",
            "io.jdev.miniprofiler.hibernate.ProfilingDatasourceConnectionProvider")

        when:
        def sf = cfg.buildSessionFactory()
        def session = sf.openSession()
        session.doWork { conn -> conn.createStatement().execute("SELECT 1") }
        session.close()
        sf.close()

        then:
        hasProfiledSql(profiler, "SELECT 1")

        cleanup:
        profiler?.stop()
    }

    private static boolean hasProfiledSql(Profiler profiler, String expectedFragment) {
        def sqlTimings = profiler.root.customTimings["sql"]
        return sqlTimings != null && sqlTimings.any { it.commandString.contains(expectedFragment) }
    }
}
