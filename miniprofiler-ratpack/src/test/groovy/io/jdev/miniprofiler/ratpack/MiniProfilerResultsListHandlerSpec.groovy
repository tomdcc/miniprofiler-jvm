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

import groovy.json.JsonSlurper
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.storage.MapStorage
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.func.Action
import ratpack.test.handling.RequestFixture
import spock.lang.Specification

import static ratpack.test.handling.RequestFixture.handle

class MiniProfilerResultsListHandlerSpec extends Specification {

    TestProfilerProvider provider
    MapStorage storage
    MiniProfilerResultsListHandler handler

    void setup() {
        provider = new TestProfilerProvider()
        storage = new MapStorage()
        provider.storage = storage
        handler = new MiniProfilerResultsListHandler(provider)
    }

    void "handler returns empty JSON array when no profiles stored"() {
        when:
        def result = handle(handler, {} as Action)

        then:
        result.sentResponse
        result.status.code == 200
        result.bodyText == '[]'
    }

    void "handler returns JSON array of profiler data"() {
        given:
        def p1 = new ProfilerImpl('test1', ProfileLevel.Info, provider)
        p1.stop()
        def p2 = new ProfilerImpl('test2', ProfileLevel.Info, provider)
        p2.stop()

        when:
        def result = handle(handler, {} as Action)

        then:
        result.sentResponse
        result.status.code == 200

        and:
        def json = new JsonSlurper().parseText(result.bodyText)
        json instanceof List
        json.size() == 2
    }

    void "handler JSON does not contain Root"() {
        given:
        def p1 = new ProfilerImpl('test1', ProfileLevel.Info, provider)
        p1.stop()

        when:
        def result = handle(handler, {} as Action)

        then:
        def json = new JsonSlurper().parseText(result.bodyText)
        !json[0].containsKey('Root')
    }

    void "handler with last-id filters results to profiles started after last-id"() {
        given:
        def p1 = new ProfilerImpl(null, 'test1', 'test1', ProfileLevel.Info, provider)
        storage.save(p1)
        Thread.sleep(10)
        def p2 = new ProfilerImpl(null, 'test2', 'test2', ProfileLevel.Info, provider)
        storage.save(p2)
        Thread.sleep(10)
        def p3 = new ProfilerImpl(null, 'test3', 'test3', ProfileLevel.Info, provider)
        storage.save(p3)

        when:
        def result = handle(handler, { RequestFixture req ->
            req.uri("?last-id=${p1.id}")
        } as Action)

        then:
        result.sentResponse
        result.status.code == 200

        and:
        def json = new JsonSlurper().parseText(result.bodyText)
        json.size() == 2
        json*.Id.contains(p2.id.toString())
        json*.Id.contains(p3.id.toString())
        !json*.Id.contains(p1.id.toString())
    }
}
