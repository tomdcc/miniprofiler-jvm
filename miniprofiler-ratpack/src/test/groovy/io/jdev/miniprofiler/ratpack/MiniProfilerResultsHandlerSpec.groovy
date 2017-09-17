/*
 * Copyright 2015 the original author or authors.
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
import io.jdev.miniprofiler.internal.JsonUtil
import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.func.Action
import ratpack.test.handling.RequestFixture
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static ratpack.http.MediaType.APPLICATION_FORM

class MiniProfilerResultsHandlerSpec extends Specification {

    @Shared
    TestProfilerProvider provider
    @Shared
    Profiler profiler

    MiniProfilerResultsHandler handler

    void setupSpec() {
        provider = new TestProfilerProvider()
        profiler = new ProfilerImpl("name", ProfileLevel.Info, provider)
        profiler.stop()
    }

    void setup() {
        handler = new MiniProfilerResultsHandler(provider)
    }

    @Unroll
    void "handler serves up result as json"() {
        when: 'ask for results'
        def result = RequestFixture.handle(handler, { RequestFixture req ->
            req.body("id=$param", APPLICATION_FORM)
        } as Action)

        then: 'response sent'
        result.sentResponse
        result.status.code == 200

        and: 'sent correct json'
        JsonUtil.toJson(profiler) == result.bodyText

        where:
        param << ["$profiler.id", "[$profiler.id]"]
    }

    void "handler serves up 404 for nonexistent result"() {
        when: 'ask for results'
        def result = RequestFixture.handle(handler, { RequestFixture req ->
            req.body("id=${UUID.randomUUID()}", APPLICATION_FORM)
        } as Action)

        then: '404 sent'
        result.sentResponse
        result.status.code == 404
    }

    void "handler serves up 400 when no form"() {
        when: 'ask for results'
        def result = RequestFixture.handle(handler, { RequestFixture req ->
        } as Action)

        then: '400 sent'
        result.sentResponse
        result.status.code == 400
    }

    void "handler serves up 400 when no id"() {
        when: 'ask for results'
        def result = RequestFixture.handle(handler, { RequestFixture req ->
            req.body("", APPLICATION_FORM)
        } as Action)

        then: '400 sent'
        result.sentResponse
        result.status.code == 400
    }

    void "handler serves up 400 when badly formed id"() {
        when: 'ask for results'
        def result = RequestFixture.handle(handler, { RequestFixture req ->
            req.body("id=foo", APPLICATION_FORM)
        } as Action)

        then: '400 sent'
        result.sentResponse
        result.status.code == 400
    }

}
