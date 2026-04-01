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

package io.jdev.miniprofiler.viewer

import io.jdev.miniprofiler.storage.Storage.ListResultsOrder
import spock.lang.Specification

import java.nio.file.Paths

class MiniProfilerViewerSingleFileStorageSpec extends Specification {

    MiniProfilerViewerSingleFileStorage storage

    void setup() {
        storage = MiniProfilerViewerSingleFileStorage.forFile(ViewerTestFixtures.PROFILE_FILE.toPath())
    }

    void "forFile parses the profile id"() {
        expect:
        storage.uuid == ViewerTestFixtures.PROFILE_ID
    }

    void "load returns profiler for matching id"() {
        when:
        def profiler = storage.load(storage.uuid)

        then:
        profiler != null
        profiler.id == storage.uuid
        profiler.name == '/test-request'
        profiler.machineName == 'testhost'
    }

    void "load returns null for unknown id"() {
        expect:
        storage.load(UUID.randomUUID()) == null
    }

    void "list returns uuid when profile started is within date range"() {
        given:
        def start = new Date(storage.load(storage.uuid).started - 1000)
        def finish = new Date(storage.load(storage.uuid).started + 1000)

        expect:
        storage.list(10, start, finish, ListResultsOrder.Ascending) == [storage.uuid]
    }

    void "list returns empty when profile started is before range"() {
        given:
        def start = new Date(storage.load(storage.uuid).started + 1000)
        def finish = new Date(storage.load(storage.uuid).started + 2000)

        expect:
        storage.list(10, start, finish, ListResultsOrder.Ascending).isEmpty()
    }

    void "list returns empty when profile started is after range"() {
        given:
        def start = new Date(storage.load(storage.uuid).started - 2000)
        def finish = new Date(storage.load(storage.uuid).started - 1000)

        expect:
        storage.list(10, start, finish, ListResultsOrder.Ascending).isEmpty()
    }

    void "forFile throws when file does not exist"() {
        when:
        MiniProfilerViewerSingleFileStorage.forFile(Paths.get('/nonexistent/path/profile.json'))

        then:
        thrown(IllegalArgumentException)
    }
}
