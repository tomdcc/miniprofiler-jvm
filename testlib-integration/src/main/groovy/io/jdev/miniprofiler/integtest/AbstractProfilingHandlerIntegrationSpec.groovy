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

package io.jdev.miniprofiler.integtest

import groovy.json.JsonSlurper
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

/**
 * Abstract base class for in-process HTTP integration tests that verify a MiniProfiler handler
 * implementation correctly serves all handler endpoints.
 *
 * <p>Subclasses implement {@link #createServer} to start the specific handler under test.</p>
 */
abstract class AbstractProfilingHandlerIntegrationSpec extends Specification {

    @Shared
    @AutoCleanup
    InProcessTestedServer server

    /**
     * Subclasses implement this to start the specific handler implementation under test.
     * The server must be ready to accept requests when this method returns.
     */
    abstract protected InProcessTestedServer createServer()

    def setupSpec() {
        server = createServer()
    }

    def setup() {
        server.clearProfiles()
    }

    private HttpURLConnection connect(String path, String accept = null) {
        HttpURLConnection conn = new URL("${server.serverUrl}${path}").openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 5_000
        conn.readTimeout = 10_000
        if (accept) {
            conn.setRequestProperty('Accept', accept)
        }
        conn
    }

    @Requires({ instance.server.profiledPagePath })
    void 'profiled request returns X-MiniProfiler-Ids header'() {
        when:
        def conn = connect(server.profiledPagePath)

        then:
        conn.responseCode == 200
        def idsHeader = conn.getHeaderField('X-MiniProfiler-Ids')
        idsHeader != null
        def ids = new JsonSlurper().parseText(idsHeader) as List
        ids.size() == 1
        UUID.fromString(ids[0] as String) != null
    }

    @Requires({ instance.server.profiledPagePath })
    void 'profiled request JSON results contain correct timing structure'() {
        when:
        def pageConn = connect(server.profiledPagePath)
        def idsHeader = pageConn.getHeaderField('X-MiniProfiler-Ids')
        def ids = new JsonSlurper().parseText(idsHeader) as List
        def conn = connect("miniprofiler/results?id=${ids[0]}", 'application/json')
        def profiler = new JsonSlurper().parse(conn.inputStream)

        then:
        conn.responseCode == 200
        conn.contentType.contains('application/json')
        profiler.Name != null
        profiler.Root != null
        profiler.Root.Children != null
        profiler.Root.Children.size() == 1
        profiler.Root.Children[0].CustomTimings?.sql?.size() == 1
    }

    void 'results endpoint returns HTML for known id'() {
        given:
        def profiler = server.profilerProvider.start('test-request')
        profiler.stop()
        def listConn = connect('miniprofiler/results-list')
        def list = new JsonSlurper().parse(listConn.inputStream) as List
        def knownId = list[0].Id

        when:
        def conn = connect("miniprofiler/results?id=${knownId}")

        then:
        conn.responseCode == 200
        conn.contentType.contains('text/html')
        conn.inputStream.text.contains('<html>')
    }

    void 'results endpoint returns 404 for unknown id'() {
        when:
        def conn = connect("miniprofiler/results?id=${UUID.randomUUID()}")

        then:
        conn.responseCode == 404
    }

    void 'results-list returns JSON array of stored profiles'() {
        given:
        def profiler = server.profilerProvider.start('test-request')
        profiler.stop()

        when:
        def conn = connect('miniprofiler/results-list')
        def results = new JsonSlurper().parse(conn.inputStream) as List

        then:
        conn.responseCode == 200
        results.size() == 1
        results[0].Id == profiler.id.toString()
        results[0].Name == 'test-request'
    }

    void 'results-list pagination with last-id filters results'() {
        given:
        def first = server.profilerProvider.start('first-request')
        first.stop()
        Thread.sleep(10)
        def second = server.profilerProvider.start('second-request')
        second.stop()

        when:
        def conn = connect("miniprofiler/results-list?last-id=${first.id.toString()}")
        def results = new JsonSlurper().parse(conn.inputStream) as List

        then:
        conn.responseCode == 200
        results.size() == 1
        results[0].Id == second.id.toString()
    }

    void 'results-index returns HTML'() {
        when:
        def conn = connect('miniprofiler/results-index')

        then:
        conn.responseCode == 200
        conn.contentType.contains('text/html')
    }

    void 'static resource is served'() {
        when:
        def conn = connect('miniprofiler/includes.min.js')

        then:
        conn.responseCode == 200
        conn.contentType.contains('javascript')
        conn.inputStream.bytes.length > 0
    }

    void 'unknown static resource returns 404'() {
        when:
        def conn = connect('miniprofiler/nonexistent.js')

        then:
        conn.responseCode == 404
    }

    @Requires({ instance.server.ajaxEndpointPath })
    void 'ajax endpoint is profiled with X-MiniProfiler-Ids header'() {
        when:
        def conn = connect(server.ajaxEndpointPath)

        then:
        conn.responseCode == 200
        def idsHeader = conn.getHeaderField('X-MiniProfiler-Ids')
        idsHeader != null
        def ids = new JsonSlurper().parseText(idsHeader) as List
        ids.size() == 1

        when:
        def jsonConn = connect("miniprofiler/results?id=${ids[0]}", 'application/json')
        def profiler = new JsonSlurper().parse(jsonConn.inputStream)

        then:
        jsonConn.responseCode == 200
        profiler.Root != null
    }
}
