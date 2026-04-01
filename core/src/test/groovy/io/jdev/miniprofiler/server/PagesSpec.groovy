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

package io.jdev.miniprofiler.server

import groovy.json.JsonSlurper
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class PagesSpec extends Specification {

    TestProfilerProvider provider = new TestProfilerProvider()

    void "renderResultListJson returns JSON array of profiler data"() {
        given:
        def p1 = new ProfilerImpl('test1', ProfileLevel.Info, provider)
        p1.stop()
        def p2 = new ProfilerImpl('test2', ProfileLevel.Info, provider)
        p2.stop()
        def ids = [p1.id, p2.id]

        when:
        def json = new JsonSlurper().parseText(Pages.renderResultListJson(ids, provider.storage))

        then:
        json instanceof List
        json.size() == 2
        json*.Name.containsAll(['test1', 'test2'])
    }

    void "renderResultListJson does not include Root"() {
        given:
        def p1 = new ProfilerImpl('test1', ProfileLevel.Info, provider)
        p1.stop()

        when:
        def json = new JsonSlurper().parseText(Pages.renderResultListJson([p1.id], provider.storage))

        then:
        !json[0].containsKey('Root')
    }

    void "renderResultListPage contains list table and listInit call"() {
        when:
        def html = Pages.renderResultListPage(provider, Optional.empty())

        then:
        html.contains('mp-results-index')
        html.contains('MiniProfiler.listInit')
    }

    void "renderResultListPage title is correct"() {
        when:
        def html = Pages.renderResultListPage(provider, Optional.empty())

        then:
        html.contains('<title>List of profiling sessions</title>')
    }

    void "renderResultListPage uses provided path"() {
        when:
        def html = Pages.renderResultListPage(provider, Optional.of('/custom/path'))

        then:
        html.contains('/custom/path/')
    }

}
