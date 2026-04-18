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

package io.jdev.miniprofiler.storage.objectstorage

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.storage.Storage
import spock.lang.Specification

import java.time.Instant

class BaseObjectStorageSpec extends Specification {

    InMemoryObjectStorage storage
    ProfilerProvider profilerProvider

    void setup() {
        storage = new InMemoryObjectStorage(new TestConfig("test-bucket", null), true)
        profilerProvider = Mock(ProfilerProvider)
    }

    def "save and load round-trip"() {
        given:
        ProfilerImpl profiler = new ProfilerImpl("test", ProfileLevel.Info, profilerProvider)

        when:
        storage.save(profiler)
        ProfilerImpl loaded = storage.load(profiler.id)

        then:
        loaded != null
        loaded.id == profiler.id
        loaded.name == profiler.name
    }

    def "load returns null for unknown id"() {
        expect:
        storage.load(UUID.randomUUID()) == null
    }

    def "save creates both profiler and index keys"() {
        given:
        ProfilerImpl profiler = new ProfilerImpl("test", ProfileLevel.Info, profilerProvider)

        when:
        storage.save(profiler)

        then:
        storage.store.keySet().any { it.startsWith("profiler/") }
        storage.store.keySet().any { it.startsWith("profiler-index/") }
    }

    def "list returns profilers in ascending order"() {
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

    def "list returns profilers in descending order"() {
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

    def "list filters by start date"() {
        given:
        def p1 = profilerStartedAt(1000L)
        def p2 = profilerStartedAt(2000L)
        [p1, p2].each { storage.save(it) }

        when:
        def ids = storage.list(10, new Date(2000L), null, Storage.ListResultsOrder.Ascending)

        then:
        ids as List == [p2.id]
    }

    def "list filters by finish date"() {
        given:
        def p1 = profilerStartedAt(1000L)
        def p2 = profilerStartedAt(2000L)
        [p1, p2].each { storage.save(it) }

        when:
        def ids = storage.list(10, null, new Date(1000L), Storage.ListResultsOrder.Ascending)

        then:
        ids as List == [p1.id]
    }

    def "list respects maxResults"() {
        given:
        (1..5).each { storage.save(profilerStartedAt(it * 1000L)) }

        when:
        def ids = storage.list(3, null, null, Storage.ListResultsOrder.Ascending)

        then:
        ids.size() == 3
    }

    def "setUnviewed marks session as unviewed"() {
        given:
        def id = UUID.randomUUID()

        when:
        storage.setUnviewed("alice", id)

        then:
        storage.getUnviewedIds("alice").contains(id)
    }

    def "setViewed removes from unviewed"() {
        given:
        def id = UUID.randomUUID()
        storage.setUnviewed("alice", id)

        when:
        storage.setViewed("alice", id)

        then:
        !storage.getUnviewedIds("alice").contains(id)
    }

    def "getUnviewedIds returns only unviewed for given user"() {
        given:
        def id1 = UUID.randomUUID()
        def id2 = UUID.randomUUID()
        storage.setUnviewed("alice", id1)
        storage.setUnviewed("bob", id2)

        expect:
        storage.getUnviewedIds("alice") as List == [id1]
        storage.getUnviewedIds("bob") as List == [id2]
    }

    def "setUnviewed with null user is no-op"() {
        when:
        storage.setUnviewed(null, UUID.randomUUID())

        then:
        notThrown(Exception)
    }

    def "setViewed with null user is no-op"() {
        when:
        storage.setViewed(null, UUID.randomUUID())

        then:
        notThrown(Exception)
    }

    def "getUnviewedIds with null user returns empty"() {
        expect:
        storage.getUnviewedIds(null).isEmpty()
    }

    def "close is idempotent"() {
        when:
        storage.close()
        storage.close()

        then:
        storage.closeClientCallCount == 1
    }

    def "close calls closeClient when ownsClient is true"() {
        given:
        def s = new InMemoryObjectStorage(new TestConfig("bucket", null), true)

        when:
        s.close()

        then:
        s.closeClientCallCount == 1
    }

    def "close does not call closeClient when ownsClient is false"() {
        given:
        def s = new InMemoryObjectStorage(new TestConfig("bucket", null), false)

        when:
        s.close()

        then:
        s.closeClientCallCount == 0
    }

    def "clear removes all data"() {
        given:
        def p = profilerStartedAt(1000L)
        storage.save(p)
        storage.setUnviewed("alice", p.id)

        when:
        storage.clear()

        then:
        storage.store.isEmpty()
        storage.getUnviewedIds("alice").isEmpty()
    }

    def "expireOlderThan removes old sessions"() {
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
        // old index key removed, recent index key still present
        !storage.store.keySet().any { it.contains(String.format("%019d", 1000L)) }
        storage.store.keySet().any { it.contains(String.format("%019d", 3000L)) }
    }

    private ProfilerImpl profilerStartedAt(long startedMs) {
        ProfilerImpl p = new ProfilerImpl("test", ProfileLevel.Info, profilerProvider)
        // reflect to set started to a known value
        def field = ProfilerImpl.getDeclaredField("started")
        field.accessible = true
        field.set(p, startedMs)
        p.stop()
        return p
    }

    // ---- Helper classes ----

    static class TestConfig extends BaseObjectStorageConfig {
        TestConfig(String bucket, String prefix) {
            super(bucket, prefix, null, null)
        }
    }

    static class InMemoryObjectStorage extends BaseObjectStorage {
        final Map<String, byte[]> store = new LinkedHashMap<>()
        int closeClientCallCount = 0

        InMemoryObjectStorage(BaseObjectStorageConfig config, boolean ownsClient) {
            super(config, ownsClient)
        }

        @Override
        protected void putObject(String key, byte[] content) {
            store[key] = content
        }

        @Override
        protected byte[] getObject(String key) {
            return store[key]
        }

        @Override
        protected void deleteObject(String key) {
            store.remove(key)
        }

        @Override
        protected Collection<String> listKeys(String keyPrefix) {
            return store.keySet().findAll { it.startsWith(keyPrefix) }
        }

        @Override
        protected void closeClient() {
            closeClientCallCount++
        }
    }
}
