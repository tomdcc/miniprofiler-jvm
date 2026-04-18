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

import spock.lang.Specification

class ObjectStorageKeysSpec extends Specification {

    def "profilerKey with prefix"() {
        given:
        def keys = new ObjectStorageKeys("myapp/")
        def id = UUID.fromString("11111111-1111-1111-1111-111111111111")

        expect:
        keys.profilerKey(id) == "myapp/profiler/11111111-1111-1111-1111-111111111111"
    }

    def "profilerKey with null prefix treated as empty string"() {
        given:
        def keys = new ObjectStorageKeys(null)
        def id = UUID.fromString("22222222-2222-2222-2222-222222222222")

        expect:
        keys.profilerKey(id) == "profiler/22222222-2222-2222-2222-222222222222"
    }

    def "indexKey zero-pads timestamp to 19 digits"() {
        given:
        def keys = new ObjectStorageKeys("")
        def id = UUID.fromString("33333333-3333-3333-3333-333333333333")

        when:
        String key = keys.indexKey(1000L, id)

        then:
        key == "profiler-index/0000000000000001000-33333333-3333-3333-3333-333333333333"
    }

    def "indexKey sorts lexicographically as chronologically"() {
        given:
        def keys = new ObjectStorageKeys("")
        def id = UUID.fromString("44444444-4444-4444-4444-444444444444")

        when:
        String earlier = keys.indexKey(1000L, id)
        String later = keys.indexKey(2000L, id)

        then:
        earlier < later
    }

    def "indexPrefix with prefix"() {
        given:
        def keys = new ObjectStorageKeys("ns/")

        expect:
        keys.indexPrefix() == "ns/profiler-index/"
    }

    def "unviewedKey with prefix"() {
        given:
        def keys = new ObjectStorageKeys("ns/")
        def id = UUID.fromString("55555555-5555-5555-5555-555555555555")

        expect:
        keys.unviewedKey("alice", id) == "ns/unviewed/alice/55555555-5555-5555-5555-555555555555"
    }

    def "unviewedPrefix"() {
        given:
        def keys = new ObjectStorageKeys("ns/")

        expect:
        keys.unviewedPrefix("bob") == "ns/unviewed/bob/"
    }

    def "allUnviewedPrefix"() {
        given:
        def keys = new ObjectStorageKeys("ns/")

        expect:
        keys.allUnviewedPrefix() == "ns/unviewed/"
    }

    def "extractIdFromIndexKey round-trip"() {
        given:
        def keys = new ObjectStorageKeys("ns/")
        def id = UUID.fromString("66666666-6666-6666-6666-666666666666")
        String key = keys.indexKey(9999999999999L, id)

        expect:
        keys.extractIdFromIndexKey(key) == id
    }

    def "extractIdFromIndexKey returns null for malformed key"() {
        given:
        def keys = new ObjectStorageKeys("")

        expect:
        keys.extractIdFromIndexKey("profiler-index/notakey") == null
        keys.extractIdFromIndexKey("profiler-index/0000000000000001000-notauuid") == null
        keys.extractIdFromIndexKey("other/key") == null
    }

    def "profilerPrefix"() {
        given:
        def keys = new ObjectStorageKeys("ns/")

        expect:
        keys.profilerPrefix() == "ns/profiler/"
    }
}
