/*
 * Copyright 2017 the original author or authors.
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
import io.jdev.miniprofiler.test.TestProfilerProvider
import org.h2.jdbcx.JdbcDataSource
import org.jooq.Batch
import org.jooq.DSLContext
import org.jooq.ExecuteContext
import org.jooq.ExecuteListenerProvider
import org.jooq.ExecuteType
import org.jooq.Query
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultExecuteListener
import spock.lang.Shared
import spock.lang.Specification

class MiniProfilerExecuteListenerSpec extends Specification {

    TestProfilerProvider tpp
    JdbcDataSource ds
    DSLContext db

    @Shared
    EnumSet<ExecuteType> testedExecuteTypes = EnumSet.noneOf(ExecuteType)

    void setup() {
        tpp = new TestProfilerProvider()

        ds = new JdbcDataSource()
        ds.URL = "jdbc:h2:mem:miniprofiler;DB_CLOSE_DELAY=-1"
        ds.user = "sa"
        ds.password = "sa"

        db = DSL.using(
            new DefaultConfiguration()
                .set(SQLDialect.H2)
                .set(ds)
                .set(new MiniProfilerExecuteListenerProvider(tpp), typeTrackingListener())
        )

        new Sql(ds).execute("create table if not exists foo (id int, name varchar(50))")
    }

    void cleanupSpec() {
        // various tests in here correspond to different values of org.jooq.ExecuteType
        // we make sure we've got test coverage for all of them
        assert testedExecuteTypes == EnumSet.allOf(ExecuteType)
    }

    void "profiles read statements made by jOOQ"() {
        when:
        def sqlStatements = profile(db.resultQuery("select * from foo where id = ?", 1))

        then:
        sqlStatements == ["select * from foo where id = 1"]
    }

    void "profiles writes made by jOOQ"() {
        when:
        def sqlStatements = profile(db.query("update foo set name = ? where id = ?", 'bar', 1))

        then:
        sqlStatements == ["update foo set name = 'bar' where id = 1"]
    }

    void "profiles ddl made by jOOQ"() {
        when:
        def sqlStatements = profile(db.query("truncate table foo"))

        then:
        sqlStatements == ["truncate table foo"]
    }

    void "profiles routines called by jOOQ"() {
        when:
        def sqlStatements = profile(db.query("call secure_rand(?)", 50))

        then:
        sqlStatements == ["call secure_rand(50)"]
    }

    void "profiles other query types executed by jOOQ"() {
        when:
        def sqlStatements = profile(db.query("set mode regular"))

        then:
        sqlStatements == ["set mode regular"]
    }

    void "profiles batch queries executed by jOOQ"() {
        when:
        def sqlStatements = profile(
            db.batch(
                db.query("insert into foo values (?, ?)", 1, 'bar'),
                db.query("update foo set name = ? where id = ?", 'baz', 1),
                db.query("delete from foo where id = ?", 1)
            )
        )

        then:
        sqlStatements == [
            "insert into foo values (1, 'bar');\n"
                + "update foo set name = 'baz' where id = 1;\n"
                + "delete from foo where id = 1"
        ]
    }

    private List<String> profile(Query query) {
        profile { -> query.execute() }
    }

    private List<String> profile(Batch batch) {
        profile { -> batch.execute() }
    }

    private List<String> profile(Runnable r) {
        def profiler = tpp.start("foo")
        r.run()
        profiler.stop()
        def timings = profiler.root.customTimings
        assert timings
        assert timings.keySet() == ['sql'] as Set
        profiler.root.customTimings.sql.commandString
    }

    private ExecuteListenerProvider typeTrackingListener() {
        { ->  new TypeTrackingExecuteListener() }
    }

    private class TypeTrackingExecuteListener extends DefaultExecuteListener {
        @Override
        void start(ExecuteContext ctx) {
            testedExecuteTypes.add(ctx.type())
        }
    }
}
