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

package io.jdev.miniprofiler.viewer

import groovy.json.JsonSlurper
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ViewerIntegSpec extends Specification {

    ForkingViewerServer server
    HttpClient client = HttpClient.newHttpClient()

    void setup() {
        server = new ForkingViewerServer(
            System.getProperty('miniprofiler.viewer.jar'),
            ViewerTestFixtures.PROFILE_FILE
        )
    }

    void cleanup() {
        server?.close()
    }

    private HttpResponse<String> get(String url, Map<String, String> headers = [:]) {
        def builder = HttpRequest.newBuilder(URI.create(url))
        headers.each { k, v -> builder.header(k, v) }
        client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    void "profile file is loaded and served as HTML"() {
        when:
        def response = get("${server.serverUrl}miniprofiler/results?id=${ViewerTestFixtures.PROFILE_ID}")

        then:
        response.statusCode() == 200
        response.body() =~ /\/test-request.*- Profiling Results/
    }

    void "profile file is served as JSON"() {
        when:
        def response = get("${server.serverUrl}miniprofiler/results?id=${ViewerTestFixtures.PROFILE_ID}", [Accept: 'application/json'])
        def profiler = new JsonSlurper().parseText(response.body())

        then:
        response.statusCode() == 200
        profiler.Name == '/test-request'
        profiler.Root != null
    }
}
