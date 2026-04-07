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

package io.jdev.miniprofiler.glassfish4.funtest

import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification

class Glassfish4ScenarioSpec extends Specification {

    @Shared
    String baseUrl = System.getProperty("scenarioTest.baseUrl") ?: 'http://127.0.0.1:8080/'

    private Map httpGet(String url, Map<String, String> headers = [:]) {
        def conn = (HttpURLConnection) new URL(url).openConnection()
        conn.connectTimeout = 5000
        conn.readTimeout = 10000
        conn.instanceFollowRedirects = false
        headers.each { k, v -> conn.setRequestProperty(k, v) }
        def statusCode = conn.responseCode
        def stream = statusCode >= 400 ? conn.errorStream : conn.inputStream
        def body = stream ? stream.getText('UTF-8') : ''
        [statusCode: statusCode, body: body, headers: conn.headerFields]
    }

    void "profiling data returned for request"() {
        when: 'hit the home page'
        def response = httpGet(baseUrl)

        then: 'response is OK with profiler IDs header'
        response.statusCode == 200
        def idsHeader = response.headers['X-MiniProfiler-Ids']
        idsHeader != null
        def ids = new JsonSlurper().parseText(idsHeader[0]) as List
        ids.size() == 1

        when: 'fetch the profiler result as JSON'
        def resultResponse = httpGet("${baseUrl}admin/miniprofiler/results?id=${ids[0]}", [Accept: 'application/json'])
        def profiler = new JsonSlurper().parseText(resultResponse.body)

        then: 'profiler has expected timing structure'
        resultResponse.statusCode == 200
        profiler.Name == '/'
        profiler.Root.Name == '/'

        and: 'EJB interceptor wraps service call'
        profiler.Root.Children.size() == 1
        profiler.Root.Children[0].Name == 'PersonServiceImpl.getAllPeople'

        and: 'service call has expected children'
        def serviceChildren = profiler.Root.Children[0].Children
        serviceChildren.size() == 2
        serviceChildren[0].Name == 'First thing'
        serviceChildren[1].Name == 'Second thing'

        and: 'Second thing has SQL custom timing'
        def secondThing = serviceChildren[1]
        secondThing.CustomTimings != null
        def sqlTimings = secondThing.CustomTimings.values().flatten()
        sqlTimings.size() == 1
        sqlTimings[0].CommandString =~ /(?i)SELECT\s+ID,\s*FIRSTNAME,\s*LASTNAME\s+FROM\s+PERSON/
    }
}
