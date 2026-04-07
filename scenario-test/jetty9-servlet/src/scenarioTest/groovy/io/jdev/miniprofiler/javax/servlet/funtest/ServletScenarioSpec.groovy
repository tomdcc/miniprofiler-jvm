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

package io.jdev.miniprofiler.javax.servlet.funtest

import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ServletScenarioSpec extends Specification {

    @Shared
    String baseUrl = System.getProperty("scenarioTest.baseUrl") ?: 'http://127.0.0.1:8080/javax-servlet/'

    @Shared
    HttpClient client = HttpClient.newHttpClient()

    private HttpResponse<String> get(String url, Map<String, String> headers = [:]) {
        def builder = HttpRequest.newBuilder(URI.create(url))
        headers.each { k, v -> builder.header(k, v) }
        client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    void "profiling data returned for request"() {
        when: 'hit the home page'
        def response = get(baseUrl)

        then: 'response is OK with profiler IDs header'
        response.statusCode() == 200
        def idsHeader = response.headers().firstValue('X-MiniProfiler-Ids')
        idsHeader.present
        def ids = new JsonSlurper().parseText(idsHeader.get()) as List
        ids.size() == 1

        when: 'fetch the profiler result as JSON'
        def resultResponse = get("${baseUrl}miniprofiler/results?id=${ids[0]}", [Accept: 'application/json'])
        def profiler = new JsonSlurper().parseText(resultResponse.body())

        then: 'profiler has expected timing structure'
        resultResponse.statusCode() == 200
        profiler.Name == '/javax-servlet/'
        profiler.DurationMilliseconds >= 0
        profiler.Root.Name == '/javax-servlet/'

        and: 'root timing has SQL custom timing'
        profiler.Root.CustomTimings != null
        profiler.Root.CustomTimings.size() >= 1
        def sqlTimings = profiler.Root.CustomTimings.values().flatten()
        sqlTimings.size() == 1
        sqlTimings[0].CommandString =~ /(?i)select\s+\*\s+from\s+people/
    }

    void "results list endpoint returns profiler entries"() {
        given: 'a profile exists'
        get(baseUrl)

        when: 'fetch the results list'
        def response = get("${baseUrl}miniprofiler/results-list")

        then:
        response.statusCode() == 200
        def results = new JsonSlurper().parseText(response.body()) as List
        results.size() >= 1
        results[0].Name != null
    }

    void "results index page returns HTML"() {
        when:
        def response = get("${baseUrl}miniprofiler/results-index")

        then:
        response.statusCode() == 200
        response.body().contains('mp-results-index')
    }

    void "single result page returns HTML"() {
        given: 'a profile exists'
        def homeResponse = get(baseUrl)
        def ids = new JsonSlurper().parseText(homeResponse.headers().firstValue('X-MiniProfiler-Ids').get()) as List

        when: 'fetch the single result page as HTML'
        def response = get("${baseUrl}miniprofiler/results?id=${ids[0]}")

        then:
        response.statusCode() == 200
        response.body() =~ /\/javax-servlet\/.*- Profiling Results/
    }
}
