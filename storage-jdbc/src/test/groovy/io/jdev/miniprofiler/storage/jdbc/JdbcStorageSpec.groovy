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

package io.jdev.miniprofiler.storage.jdbc

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.storage.Storage
import io.jdev.miniprofiler.storage.jdbc.dialect.H2Dialect
import org.h2.jdbcx.JdbcDataSource
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource
import java.time.Instant

class JdbcStorageSpec extends Specification {

    @Shared ProfilerProvider profilerProvider = Mock(ProfilerProvider)

    @AutoCleanup
    JdbcStorage storage

    DataSource dataSource

    void setup() {
        def h2ds = new JdbcDataSource()
        h2ds.url = "jdbc:h2:mem:test-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        dataSource = h2ds
        storage = new JdbcStorage(dataSource, new H2Dialect())
        storage.createTable()
    }

    void "save and load round-trip"() {
        given:
        def profiler = newProfiler("test")

        when:
        storage.save(profiler)
        def loaded = storage.load(profiler.id)

        then:
        loaded != null
        loaded.id == profiler.id
        loaded.name == profiler.name
    }

    void "load returns null for unknown id"() {
        expect:
        storage.load(UUID.randomUUID()) == null
    }

    void "list returns profilers in ascending order"() {
        given:
        def p1 = profilerStartedAt(1000L)
        def p2 = profilerStartedAt(2000L)
        def p3 = profilerStartedAt(3000L)
        [p1, p2, p3].each { storage.save(it) }

        when:
        def ids = storage.list(10, null, null, Storage.ListResultsOrder.Ascending)

        then:
        ids as List == [p1.id, p2.id, p3.id]
    }

    void "list returns profilers in descending order"() {
        given:
        def p1 = profilerStartedAt(1000L)
        def p2 = profilerStartedAt(2000L)
        def p3 = profilerStartedAt(3000L)
        [p1, p2, p3].each { storage.save(it) }

        when:
        def ids = storage.list(10, null, null, Storage.ListResultsOrder.Descending)

        then:
        ids as List == [p3.id, p2.id, p1.id]
    }

    void "list filters by date range"() {
        given:
        def p1 = profilerStartedAt(1000L)
        def p2 = profilerStartedAt(5000L)
        def p3 = profilerStartedAt(9000L)
        [p1, p2, p3].each { storage.save(it) }

        when:
        def ids = storage.list(10, new Date(2000L), new Date(8000L), Storage.ListResultsOrder.Ascending)

        then:
        ids as List == [p2.id]
    }

    void "list respects maxResults"() {
        given:
        (1..5).each { storage.save(profilerStartedAt(it * 1000L)) }

        when:
        def ids = storage.list(3, null, null, Storage.ListResultsOrder.Ascending)

        then:
        ids.size() == 3
    }

    void "setViewed and setUnviewed update the viewed flag"() {
        given:
        def profiler = newProfiler("test")
        profiler.user = "alice"
        storage.save(profiler)

        when:
        storage.setUnviewed("alice", profiler.id)

        then:
        storage.getUnviewedIds("alice").contains(profiler.id)

        when:
        storage.setViewed("alice", profiler.id)

        then:
        !storage.getUnviewedIds("alice").contains(profiler.id)
    }

    void "getUnviewedIds returns only unviewed for given user"() {
        given:
        def p1 = newProfiler("test1")
        p1.user = "alice"
        def p2 = newProfiler("test2")
        p2.user = "bob"
        storage.save(p1)
        storage.save(p2)
        storage.setUnviewed("alice", p1.id)
        storage.setUnviewed("bob", p2.id)

        expect:
        storage.getUnviewedIds("alice") as Set == [p1.id] as Set
        storage.getUnviewedIds("bob") as Set == [p2.id] as Set
    }

    void "setUnviewed with null user is a no-op"() {
        given:
        def profiler = newProfiler("test")
        storage.save(profiler)

        when:
        storage.setUnviewed(null, profiler.id)

        then:
        noExceptionThrown()
    }

    void "getUnviewedIds with null user returns empty"() {
        expect:
        storage.getUnviewedIds(null).isEmpty()
    }

    void "save updates existing profiler on second save"() {
        given: "a profiler saved without client timings"
        def profiler = newProfiler("test")
        storage.save(profiler)
        def loadedBefore = storage.load(profiler.id)
        def jsonBefore = loadedBefore.toJSONString()

        when: "the profiler is saved again with modified data"
        profiler.machineName = "updated-host"
        storage.save(profiler)
        def loadedAfter = storage.load(profiler.id)

        then: "the stored JSON reflects the update"
        loadedAfter.machineName == "updated-host"
        loadedAfter.toJSONString() != jsonBefore
    }

    void "clear removes all data"() {
        given:
        def p = profilerStartedAt(1000L)
        storage.save(p)

        when:
        storage.clear()

        then:
        storage.load(p.id) == null
        storage.list(10, null, null, Storage.ListResultsOrder.Ascending).isEmpty()
    }

    void "expireOlderThan removes old sessions"() {
        given:
        def old = profilerStartedAt(1000L)
        def recent = profilerStartedAt(3000L)
        storage.save(old)
        storage.save(recent)

        when:
        storage.expireOlderThan(Instant.ofEpochMilli(2000L))

        then:
        storage.load(old.id) == null
        storage.load(recent.id) != null
    }

    void "close is idempotent"() {
        when:
        storage.close()
        storage.close()

        then:
        noExceptionThrown()
    }

    void "close without owned datasource does not close it"() {
        given:
        def ds = new JdbcDataSource()
        ds.url = "jdbc:h2:mem:close-test-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        def s = new JdbcStorage(ds, new H2Dialect())
        s.createTable()

        when:
        s.close()

        then: "datasource is still usable"
        def conn = ds.getConnection()
        conn.close()
    }

    void "getCreateTableSql returns valid DDL"() {
        expect:
        def ddl = storage.getCreateTableSql()
        ddl.contains("CREATE TABLE")
        ddl.contains("mini_profiler_sessions")
        ddl.contains("profiler_id")
        ddl.contains("profile_json")
    }

    void "createTable is idempotent"() {
        when:
        storage.createTable()
        storage.createTable()

        then:
        noExceptionThrown()
    }

    void "auto-detects H2 dialect from datasource"() {
        given:
        def ds = new JdbcDataSource()
        ds.url = "jdbc:h2:mem:detect-test-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"

        when:
        def s = new JdbcStorage(ds)

        then:
        noExceptionThrown()

        cleanup:
        s?.close()
    }

    private ProfilerImpl newProfiler(String name) {
        def profiler = new ProfilerImpl(name, ProfileLevel.Info, profilerProvider)
        profiler.stop()
        return profiler
    }

    private ProfilerImpl profilerStartedAt(long startedMs) {
        ProfilerImpl p = new ProfilerImpl("test", ProfileLevel.Info, profilerProvider)
        def field = ProfilerImpl.getDeclaredField("started")
        field.accessible = true
        field.set(p, startedMs)
        p.stop()
        return p
    }
}
