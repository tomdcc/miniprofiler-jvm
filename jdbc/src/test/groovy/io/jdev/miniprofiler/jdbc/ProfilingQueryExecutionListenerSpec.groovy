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

package io.jdev.miniprofiler.jdbc

import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.internal.NullProfiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import net.ttddyy.dsproxy.ExecutionInfo
import net.ttddyy.dsproxy.QueryInfo
import org.h2.jdbcx.JdbcDataSource
import spock.lang.Specification

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

class ProfilingQueryExecutionListenerSpec extends Specification {

    TestProfilerProvider tpp
    ProfilingDataSource pds
    Profiler profiler

    void setup() {
        def raw = new JdbcDataSource()
        raw.URL = "jdbc:h2:mem:miniprofiler_jdbc_test;DB_CLOSE_DELAY=-1"
        raw.user = "sa"
        raw.password = "sa"

        // Reset schema between tests
        try (Connection c = raw.connection; Statement s = c.createStatement()) {
            s.executeUpdate("drop table if exists people")
            s.executeUpdate("create table people (id bigint auto_increment primary key, name varchar(255))")
        }

        tpp = new TestProfilerProvider()
        pds = new ProfilingDataSource(raw, tpp)
        profiler = tpp.start("test")
    }

    void cleanup() {
        profiler?.stop()
    }

    private List<String> recordedSql() {
        def sqlTimings = profiler.root.customTimings?.sql
        sqlTimings == null ? [] : sqlTimings.collect { it.commandString }
    }

    private List<Long> recordedDurations() {
        def sqlTimings = profiler.root.customTimings?.sql
        sqlTimings == null ? [] : sqlTimings.collect { it.durationMilliseconds }
    }

    void "records a plain Statement query"() {
        when:
        try (Connection c = pds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("insert into people(name) values ('Tom')")
        }

        then:
        recordedSql() == ["insert into people(name) values ('Tom')"]
        recordedDurations().size() == 1
        recordedDurations()[0] >= 0
    }

    void "records a PreparedStatement with parameters interpolated"() {
        when:
        try (Connection c = pds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("insert into people(name) values (?)")) {
                ps.setString(1, "Alice")
                ps.executeUpdate()
            }
            try (PreparedStatement ps = c.prepareStatement("select id from people where name = ?")) {
                ps.setString(1, "Alice")
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next()
                }
            }
        }

        then:
        recordedSql() == [
            "insert into people(name) values ('Alice')",
            "select id from people where name = 'Alice'"
        ]
    }

    void "interpolates numeric and null parameters"() {
        when:
        try (Connection c = pds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("insert into people(id, name) values (?, ?)")) {
                ps.setLong(1, 42L)
                ps.setNull(2, java.sql.Types.VARCHAR)
                ps.executeUpdate()
            }
        }

        then:
        recordedSql() == ["insert into people(id, name) values (42, NULL)"]
    }

    void "escapes single quotes in string parameters"() {
        when:
        try (Connection c = pds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("insert into people(name) values (?)")) {
                ps.setString(1, "O'Brien")
                ps.executeUpdate()
            }
        }

        then:
        recordedSql() == ["insert into people(name) values ('O''Brien')"]
    }

    void "records custom timings with type=sql and subtype=query"() {
        when:
        try (Connection c = pds.getConnection(); Statement s = c.createStatement()) {
            s.executeQuery("select 1").close()
        }

        then:
        def sqlTimings = profiler.root.customTimings.sql
        sqlTimings.size() == 1
        sqlTimings[0].executeType == "query"
    }

    void "skips SQL rendering when the current profiler is not active"() {
        given: 'a provider that returns the NullProfiler'
        def nullProviding = Stub(ProfilerProvider) {
            current() >> NullProfiler.INSTANCE
        }
        def listener = new ProfilingQueryExecutionListener(nullProviding)

        and: 'a QueryInfo whose getQuery() must never be called'
        def queryInfo = Mock(QueryInfo)
        def execInfo = Mock(ExecutionInfo)

        when:
        listener.afterQuery(execInfo, [queryInfo])

        then: 'the listener returned without inspecting the query'
        0 * queryInfo._
        0 * execInfo._
    }

    void "records a single timing for a batch with concatenated queries"() {
        when:
        try (Connection c = pds.getConnection(); Statement s = c.createStatement()) {
            s.addBatch("insert into people(name) values ('A')")
            s.addBatch("insert into people(name) values ('B')")
            s.addBatch("insert into people(name) values ('C')")
            s.executeBatch()
        }

        then:
        recordedSql() == [
            "insert into people(name) values ('A')\n\n" +
            "insert into people(name) values ('B')\n\n" +
            "insert into people(name) values ('C')"
        ]
        recordedDurations().size() == 1
    }
}
