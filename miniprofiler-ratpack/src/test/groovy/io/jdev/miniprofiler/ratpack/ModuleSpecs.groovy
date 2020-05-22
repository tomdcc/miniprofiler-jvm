/*
 * Copyright 2015 the original author or authors.
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

package io.jdev.miniprofiler.ratpack

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.jdev.miniprofiler.sql.ProfilingDataSource
import org.h2.jdbcx.JdbcConnectionPool
import org.h2.jdbcx.JdbcDataSource
import ratpack.server.ServerConfig
import spock.lang.Specification

import javax.sql.DataSource

class ModuleSpecs extends Specification {

    static interface TypedDataSource extends DataSource { }

    static class ServerConfigModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ServerConfig).toInstance(ServerConfig.embedded().build())
        }
    }

    JdbcDataSource ds = new JdbcDataSource(url: "jdbc:h2:mem:dev")

    void "hikari module provides wrapped ds"() {
        given:
        DataSource ds = new JdbcDataSource(url: "jdbc:h2:mem:dev")
        Injector injector = Guice.createInjector(new ServerConfigModule(), new MiniProfilerHikariModule() {
            @Override
            protected void defaultConfig(ServerConfig serverConfig, HikariConfig config) {
                config.dataSource = ds
            }
        })

        when:
        def returned = injector.getInstance(DataSource)

        then:
        returned instanceof ProfilingDataSource

        and:
        returned.unwrap(HikariDataSource)
        returned.unwrap(JdbcDataSource) == ds
    }

    void "hikari module provides original ds if profiling suppressed"() {
        given:
        Injector injector = Guice.createInjector(new ServerConfigModule(), new MiniProfilerHikariModule(true) {
            @Override
            protected void defaultConfig(ServerConfig serverConfig, HikariConfig config) {
                config.dataSource = ds
            }
        })

        when:
        def returned = injector.getInstance(DataSource)

        then:
        returned instanceof HikariDataSource
        returned.unwrap(JdbcDataSource) == ds
    }

    void "h2 module provides wrapped ds"() {
        given:
        Injector injector = Guice.createInjector(new MiniProfilerH2Module())

        when:
        def returned = injector.getInstance(DataSource)

        then:
        returned instanceof ProfilingDataSource

        and:
        returned.unwrap(JdbcConnectionPool)
    }

    void "h2 module provides original ds if profiling suppressed"() {
        given:
        Injector injector = Guice.createInjector(new MiniProfilerH2Module(true))

        when:
        def returned = injector.getInstance(DataSource)

        then:
        returned instanceof JdbcConnectionPool
    }
}
