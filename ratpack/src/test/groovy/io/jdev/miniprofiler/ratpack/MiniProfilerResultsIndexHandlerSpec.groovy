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

import io.jdev.miniprofiler.test.TestProfilerProvider
import ratpack.func.Action
import spock.lang.Specification

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML
import static ratpack.test.handling.RequestFixture.handle

class MiniProfilerResultsIndexHandlerSpec extends Specification {

    TestProfilerProvider provider
    MiniProfilerResultsIndexHandler handler

    void setup() {
        provider = new TestProfilerProvider()
        handler = new MiniProfilerResultsIndexHandler(provider)
    }

    void "handler returns 200 HTML page with results index table"() {
        when:
        def result = handle(handler, {} as Action)

        then:
        result.sentResponse
        result.status.code == 200
        result.headers[CONTENT_TYPE as String] == TEXT_HTML as String

        and: 'contains list table'
        result.bodyText.contains("mp-results-index")

        and: 'contains listInit call'
        result.bodyText.contains("MiniProfiler.listInit")
    }

    void "handler renders page title correctly"() {
        when:
        def result = handle(handler, {} as Action)

        then:
        result.bodyText.contains("List of profiling sessions")
    }
}
