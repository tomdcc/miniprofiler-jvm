/*
 * Copyright 2013 the original author or authors.
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

package io.jdev.miniprofiler.storage

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.storage.Storage.ListResultsOrder
import spock.lang.Specification

class MapStorageSpec extends Specification {

    MapStorage storage
    ProfilerProvider profilerProvider

    void setup() {
        storage = new MapStorage(2)
        profilerProvider = Mock(ProfilerProvider)
    }

    void "map storage returns saved value"() {
        given:
        def val = new ProfilerImpl('test', ProfileLevel.Info, profilerProvider)

        when:
        storage.save(val)

        then:
        storage.load(val.id) == val
    }

    void "map storage does not grow boundlessly"() {
        given:
        def val1 = new ProfilerImpl('test1', ProfileLevel.Info, profilerProvider)
        def val2 = new ProfilerImpl('test2', ProfileLevel.Info, profilerProvider)
        def val3 = new ProfilerImpl('test3', ProfileLevel.Info, profilerProvider)

        when:
        storage.save(val1)
        storage.save(val2)
        storage.save(val3)

        then:
        storage.cache.size() == 2
        !storage.load(val1.id)
        storage.load(val2.id) == val2
        storage.load(val3.id) == val3
    }

    void "list returns profiles in descending order by started time"() {
        given:
        storage = new MapStorage(10)
        def val1 = new ProfilerImpl(null, 'test1', 'test1', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val2 = new ProfilerImpl(null, 'test2', 'test2', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val3 = new ProfilerImpl(null, 'test3', 'test3', ProfileLevel.Info, profilerProvider)
        storage.save(val1)
        storage.save(val2)
        storage.save(val3)

        when:
        def result = storage.list(10, null, null, ListResultsOrder.Descending)

        then:
        result.toList() == [val3.id, val2.id, val1.id]
    }

    void "list returns profiles in ascending order by started time"() {
        given:
        storage = new MapStorage(10)
        def val1 = new ProfilerImpl(null, 'test1', 'test1', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val2 = new ProfilerImpl(null, 'test2', 'test2', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val3 = new ProfilerImpl(null, 'test3', 'test3', ProfileLevel.Info, profilerProvider)
        storage.save(val1)
        storage.save(val2)
        storage.save(val3)

        when:
        def result = storage.list(10, null, null, ListResultsOrder.Ascending)

        then:
        result.toList() == [val1.id, val2.id, val3.id]
    }

    void "list respects maxResults limit"() {
        given:
        storage = new MapStorage(10)
        def val1 = new ProfilerImpl(null, 'test1', 'test1', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val2 = new ProfilerImpl(null, 'test2', 'test2', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val3 = new ProfilerImpl(null, 'test3', 'test3', ProfileLevel.Info, profilerProvider)
        storage.save(val1)
        storage.save(val2)
        storage.save(val3)

        when:
        def result = storage.list(2, null, null, ListResultsOrder.Descending)

        then:
        result.size() == 2
        result.toList() == [val3.id, val2.id]
    }

    void "list filters by start date"() {
        given:
        storage = new MapStorage(10)
        def val1 = new ProfilerImpl(null, 'test1', 'test1', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val2 = new ProfilerImpl(null, 'test2', 'test2', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val3 = new ProfilerImpl(null, 'test3', 'test3', ProfileLevel.Info, profilerProvider)
        storage.save(val1)
        storage.save(val2)
        storage.save(val3)

        when:
        // Filter to only val2 and val3 by starting after val1
        def result = storage.list(10, new Date(val2.started), null, ListResultsOrder.Descending)

        then:
        result.toList() == [val3.id, val2.id]
    }

    void "list filters by finish date"() {
        given:
        storage = new MapStorage(10)
        def val1 = new ProfilerImpl(null, 'test1', 'test1', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val2 = new ProfilerImpl(null, 'test2', 'test2', ProfileLevel.Info, profilerProvider)
        Thread.sleep(10)
        def val3 = new ProfilerImpl(null, 'test3', 'test3', ProfileLevel.Info, profilerProvider)
        storage.save(val1)
        storage.save(val2)
        storage.save(val3)

        when:
        // Filter to only val1 and val2 by ending before val3
        def result = storage.list(10, null, new Date(val2.started), ListResultsOrder.Descending)

        then:
        result.toList() == [val2.id, val1.id]
    }
}
