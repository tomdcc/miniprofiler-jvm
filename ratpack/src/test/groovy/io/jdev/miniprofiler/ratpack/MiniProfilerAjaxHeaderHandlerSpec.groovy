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

package io.jdev.miniprofiler.ratpack

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.storage.MapStorage
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.func.Action
import ratpack.test.handling.RequestFixture
import spock.lang.Specification

class MiniProfilerAjaxHeaderHandlerSpec extends Specification {

    TestProfilerProvider provider
    MapStorage storage
    MiniProfilerAjaxHeaderHandler handler

    void setup() {
        provider = new TestProfilerProvider()
        storage = (MapStorage) provider.storage
        handler = new MiniProfilerAjaxHeaderHandler(provider)
    }

    void "header contains only current id when no user"() {
        given:
        def profiler = new ProfilerImpl('/test', ProfileLevel.Info, provider)

        when:
        def result = RequestFixture.handle(handler, { RequestFixture req ->
            req.registry { r -> r.add(Profiler, profiler) }
        } as Action)

        then:
        result.headers.get('X-MiniProfiler-Ids') == "[\"${profiler.id}\"]"
    }

    void "header includes previously unviewed ids for authenticated user"() {
        given:
        def profiler = new ProfilerImpl('/test', ProfileLevel.Info, provider)
        profiler.setUser('alice')
        def previousProfiler = new ProfilerImpl('/previous', ProfileLevel.Info, provider)
        previousProfiler.stop()
        storage.save(previousProfiler)
        storage.setUnviewed('alice', previousProfiler.id)

        when:
        def result = RequestFixture.handle(handler, { RequestFixture req ->
            req.registry { r -> r.add(Profiler, profiler) }
        } as Action)

        then:
        def header = result.headers.get('X-MiniProfiler-Ids')
        header.contains("\"${profiler.id}\"")
        header.contains("\"${previousProfiler.id}\"")
    }

    void "header caps previously unviewed ids at maxUnviewedProfiles"() {
        given:
        provider.uiConfig.maxUnviewedProfiles = 2
        def profiler = new ProfilerImpl('/test', ProfileLevel.Info, provider)
        profiler.setUser('alice')

        and: 'add 5 unviewed profiles'
        5.times {
            def p = new ProfilerImpl("/old-$it", ProfileLevel.Info, provider)
            p.stop()
            storage.save(p)
            storage.setUnviewed('alice', p.id)
        }

        when:
        def result = RequestFixture.handle(handler, { RequestFixture req ->
            req.registry { r -> r.add(Profiler, profiler) }
        } as Action)

        then: 'header contains current id + at most 2 previous ids'
        def header = result.headers.get('X-MiniProfiler-Ids')
        def ids = header.replaceAll('[\\[\\]"]', '').split(',')
        ids.length <= 3
        ids[0] == profiler.id.toString()
    }
}
