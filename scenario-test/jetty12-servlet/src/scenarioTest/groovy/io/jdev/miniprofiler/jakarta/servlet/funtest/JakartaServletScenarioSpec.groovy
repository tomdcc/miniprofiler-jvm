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

package io.jdev.miniprofiler.jakarta.servlet.funtest

import io.jdev.miniprofiler.integtest.TestMiniProfilerHttpClient
import spock.lang.Shared
import spock.lang.Specification

class JakartaServletScenarioSpec extends Specification {

    @Shared
    String baseUrl = System.getProperty("scenarioTest.baseUrl") ?: 'http://127.0.0.1:8080/jakarta-servlet/'

    @Shared
    TestMiniProfilerHttpClient client = new TestMiniProfilerHttpClient(baseUrl)

    void "profiling data returned for request"() {
        when: 'hit the home page'
        def response = client.get('')

        then: 'response is OK with profiler IDs header'
        response.statusCode() == 200
        response.miniProfilerIds().size() == 1

        when: 'fetch the profiler result as JSON'
        def resultResponse = client.awaitResultsJson(response.miniProfilerId())
        def profiler = resultResponse.bodyAsJson()

        then: 'profiler has expected timing structure'
        resultResponse.statusCode() == 200
        profiler.Name == '/jakarta-servlet/'
        profiler.DurationMilliseconds >= 0
        profiler.Root.Name == '/jakarta-servlet/'

        and: 'root timing has SQL custom timing'
        profiler.Root.CustomTimings != null
        profiler.Root.CustomTimings.size() >= 1
        def sqlTimings = profiler.Root.CustomTimings.values().flatten()
        sqlTimings.size() == 1
        // miniprofiler.properties sets sql.format.uppercase=true and sql.format.indent.width=4
        sqlTimings[0].CommandString == 'SELECT\n    *\nFROM\n    people'
    }

    void "results list endpoint returns profiler entries"() {
        given: 'a profile exists'
        client.get('')

        when: 'fetch the results list'
        def response = client.getResultsList()

        then:
        response.statusCode() == 200
        def results = response.bodyAsJson() as List
        results.size() >= 1
        results[0].Name != null
    }

    void "results index page returns HTML"() {
        when:
        def response = client.getResultsIndex()

        then:
        response.statusCode() == 200
        response.body().contains('mp-results-index')
    }

    void "single result page returns HTML"() {
        given: 'a profile exists'
        def homeResponse = client.get('')

        when: 'fetch the single result page as HTML'
        def response = client.awaitResultsHtml(homeResponse.miniProfilerId())

        then:
        response.statusCode() == 200
        response.body() =~ /\/jakarta-servlet\/.*- Profiling Results/
    }
}
