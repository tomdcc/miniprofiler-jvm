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

package io.jdev.miniprofiler.jooq

import groovy.sql.Sql
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import spock.lang.Specification

class JooqCrossVersionSpec extends Specification {

    TestProfilerProvider profilerProvider
    JdbcDataSource ds
    DSLContext db

    void setup() {
        profilerProvider = new TestProfilerProvider()

        ds = new JdbcDataSource()
        ds.URL = "jdbc:h2:mem:jooq_cross_version_test;DB_CLOSE_DELAY=-1"
        ds.user = "sa"
        ds.password = "sa"

        // DefaultConfiguration.set(DataSource) was added after 3.0, so we use
        // DSL.using(DataSource, SQLDialect) and then set the listener on the live config.
        db = DSL.using(ds, SQLDialect.H2)
        db.configuration().set(new MiniProfilerExecuteListenerProvider(profilerProvider))

        new Sql(ds).execute("create table if not exists foo (id int, name varchar(50))")
    }

    void "profiles SELECT queries and inlines parameters"() {
        given:
        new Sql(ds).execute("delete from foo")
        new Sql(ds).execute("insert into foo values (1, 'test')")
        def profiler = profilerProvider.start("select-test")

        when:
        db.resultQuery("select * from foo where id = ?", 1).fetch()

        then:
        hasProfiledSql(profiler, "select * from foo where id = 1")

        cleanup:
        profiler?.stop()
    }

    void "profiles INSERT queries and inlines parameters"() {
        given:
        def profiler = profilerProvider.start("insert-test")

        when:
        db.query("insert into foo values (?, ?)", 2, "bar").execute()

        then:
        hasProfiledSql(profiler, "insert into foo values (2, 'bar')")

        cleanup:
        profiler?.stop()
    }

    void "profiles batch queries"() {
        given:
        def profiler = profilerProvider.start("batch-test")

        when:
        db.batch(
            db.query("insert into foo values (?, ?)", 10, "alpha"),
            db.query("insert into foo values (?, ?)", 11, "beta")
        ).execute()

        then:
        def sqlTimings = profiler.root.customTimings["sql"]
        sqlTimings != null && sqlTimings.size() == 1
        sqlTimings[0].commandString.contains("insert into foo values (10, 'alpha')")
        sqlTimings[0].commandString.contains("insert into foo values (11, 'beta')")

        cleanup:
        profiler?.stop()
    }

    private static boolean hasProfiledSql(Profiler profiler, String expectedSql) {
        def sqlTimings = profiler.root.customTimings["sql"]
        return sqlTimings != null && sqlTimings.any { it.commandString == expectedSql }
    }
}
