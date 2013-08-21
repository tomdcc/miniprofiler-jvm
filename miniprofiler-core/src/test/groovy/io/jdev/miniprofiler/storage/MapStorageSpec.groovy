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
import io.jdev.miniprofiler.ProfilerImpl
import io.jdev.miniprofiler.ProfilerProvider
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
}
