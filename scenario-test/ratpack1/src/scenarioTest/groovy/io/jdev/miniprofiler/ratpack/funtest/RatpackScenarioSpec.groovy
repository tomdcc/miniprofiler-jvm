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

package io.jdev.miniprofiler.ratpack.funtest

import groovy.json.JsonSlurper
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.ServerBackedApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class RatpackScenarioSpec extends Specification {

    @AutoCleanup
    @Shared
    ServerBackedApplicationUnderTest aut

    @Shared
    String baseUrl

    @Shared
    HttpClient client = HttpClient.newHttpClient()

    void setupSpec() {
        aut = new MainClassApplicationUnderTest(Main)
        baseUrl = aut.address.toString()
    }

    private HttpResponse<String> get(String url, Map<String, String> headers = [:]) {
        def builder = HttpRequest.newBuilder(URI.create(url))
        headers.each { k, v -> builder.header(k, v) }
        client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    void "profiling data returned for page request"() {
        when: 'hit the page endpoint'
        def response = get("${baseUrl}page")

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
        profiler.Name == '/page'
        profiler.Root.Name == '/page'

        and: 'root has expected children'
        profiler.Root.Children.size() == 2
        profiler.Root.Children[0].Name == 'TestHandler.handle'
        profiler.Root.Children[1].Name == 'TestHandler.getData'

        and: 'getData has SQL custom timing'
        def getData = profiler.Root.Children[1]
        getData.CustomTimings != null
        def sqlTimings = getData.CustomTimings.values().flatten()
        sqlTimings.size() == 1
        sqlTimings[0].CommandString =~ /(?i)select\s+\*\s+from\s+people/
    }

    void "second request also produces profiling data"() {
        when: 'hit the page endpoint twice'
        get("${baseUrl}page")
        def secondResponse = get("${baseUrl}page")

        then: 'second response also has profiler IDs header'
        secondResponse.statusCode() == 200
        def idsHeader = secondResponse.headers().firstValue('X-MiniProfiler-Ids')
        idsHeader.present
        def ids = new JsonSlurper().parseText(idsHeader.get()) as List
        ids.size() >= 1
    }

    void "results list endpoint returns profiler entries"() {
        given: 'a profile exists'
        get("${baseUrl}page")

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
        def homeResponse = get("${baseUrl}page")
        def ids = new JsonSlurper().parseText(homeResponse.headers().firstValue('X-MiniProfiler-Ids').get()) as List

        when: 'fetch the single result page as HTML'
        def response = get("${baseUrl}miniprofiler/results?id=${ids[0]}")

        then:
        response.statusCode() == 200
        response.body() =~ /\/page.*- Profiling Results/
    }
}
