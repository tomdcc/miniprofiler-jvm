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
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.AutoCleanup
import spock.lang.Specification

class MiniProfilerServerSpec extends Specification {

    static final String APPLICATION_JSON = 'application/json'
    static final String TEXT_HTML = 'text/html'

    TestProfilerProvider provider = new TestProfilerProvider()
    Profiler profiler

    @AutoCleanup
    MiniProfilerServer server = new MiniProfilerServer(provider)

    void setup() {
        profiler = provider.start('test').tap {
            stop()
        }
    }

    void cleanup() {
        server?.close()
    }

    private HttpURLConnection connect(String path, String accept = null, String method = 'GET', String body = null) {
        def conn = new URL("http://127.0.0.1:${server.port}/${path}").openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        if (accept) { conn.setRequestProperty('Accept', accept) }
        if (method != 'GET') { conn.requestMethod = method }
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.withWriter('UTF-8') { it.write(body) }
        }
        conn
    }

    void "port and baseUrl reflect server address"() {
        expect:
        server.port > 0
        server.baseUrl == "http://127.0.0.1:${server.port}/"
    }

    void "GET results without Accept returns HTML single result page"() {
        when:
        def conn = connect("miniprofiler/results?id=${profiler.id}")

        then:
        conn.responseCode == 200
        conn.contentType.contains(TEXT_HTML)
        conn.inputStream.text.contains('<html>')
    }

    void "GET results with JSON Accept returns profiler JSON"() {
        when:
        def conn = connect("miniprofiler/results?id=${profiler.id}", APPLICATION_JSON)
        def json = new JsonSlurper().parse(conn.inputStream)

        then:
        conn.responseCode == 200
        conn.contentType.contains(APPLICATION_JSON)
        json.Id == profiler.id.toString()
        json.Name == 'test'
    }

    void "POST results with JSON body returns profiler JSON"() {
        when:
        def conn = connect('miniprofiler/results', APPLICATION_JSON, 'POST', """{"Id": "${profiler.id}"}""")
        def json = new JsonSlurper().parse(conn.inputStream)

        then:
        conn.responseCode == 200
        conn.contentType.contains(APPLICATION_JSON)
        json.Id == profiler.id.toString()
    }

    void "GET results with unknown id returns 404"() {
        when:
        def conn = connect("miniprofiler/results?id=${UUID.randomUUID()}")

        then:
        conn.responseCode == 404
    }

    void "GET results with no id returns 400"() {
        when:
        def conn = connect('miniprofiler/results')

        then:
        conn.responseCode == 400
    }

    void "DELETE results returns 405"() {
        when:
        def conn = connect("miniprofiler/results?id=${profiler.id}", null, 'DELETE')

        then:
        conn.responseCode == 405
    }

    // ---- results-index ---------------------------------------------------

    void "GET results-index returns HTML list page"() {
        when:
        def conn = connect('miniprofiler/results-index')

        then:
        conn.responseCode == 200
        conn.contentType.contains(TEXT_HTML)
        conn.inputStream.text.contains('<html>')
    }

    void "POST results-index returns 405"() {
        when:
        def conn = connect('miniprofiler/results-index', null, 'POST', '')

        then:
        conn.responseCode == 405
    }

    // ---- results-list ----------------------------------------------------

    void "GET results-list returns JSON array of all results"() {
        when:
        def conn = connect('miniprofiler/results-list')
        def json = new JsonSlurper().parse(conn.inputStream)

        then:
        conn.responseCode == 200
        conn.contentType.contains(APPLICATION_JSON)
        json instanceof List
        json.size() == 1
        json[0].Id == profiler.id.toString()
    }

    void "GET results-list with last-id returns only results newer than the given profiler"() {
        given:
        Thread.sleep(5)
        Profiler p2 = provider.start('second')
        p2.stop()
        Thread.sleep(5)
        Profiler p3 = provider.start('third')
        p3.stop()

        when:
        def conn = connect("miniprofiler/results-list?last-id=${profiler.id}")
        def json = new JsonSlurper().parse(conn.inputStream)

        then:
        conn.responseCode == 200
        json instanceof List
        json.size() == 2
        json*.Id.contains(p2.id.toString())
        json*.Id.contains(p3.id.toString())
        !json*.Id.contains(profiler.id.toString())
    }

    void "POST results-list returns 405"() {
        when:
        def conn = connect('miniprofiler/results-list', null, 'POST', '')

        then:
        conn.responseCode == 405
    }

    void "handleResults marks profiler as viewed"() {
        given: 'a profiler with a known user, stored and marked unviewed'
        Profiler userProfiler = provider.start('user-request')
        (userProfiler as io.jdev.miniprofiler.internal.ProfilerImpl).user = 'alice'
        userProfiler.stop()
        provider.storage.setUnviewed('alice', userProfiler.id)

        when: 'results are fetched'
        def conn = connect("miniprofiler/results?id=${userProfiler.id}", APPLICATION_JSON)
        conn.responseCode  // consume

        then: 'profiler is no longer in unviewed set'
        provider.storage.getUnviewedIds('alice').empty
    }

    // ---- static resources ------------------------------------------------

    void "GET static resource returns content with correct content type"() {
        when:
        def conn = connect('miniprofiler/includes.min.js')

        then:
        conn.responseCode == 200
        conn.contentType.contains('javascript')
        conn.inputStream.bytes.length > 0
    }

    void "GET unknown static resource returns 404"() {
        when:
        def conn = connect('miniprofiler/nonexistent.js')

        then:
        conn.responseCode == 404
    }

    void "POST static resource path returns 405"() {
        when:
        def conn = connect('miniprofiler/includes.min.js', null, 'POST', '')

        then:
        conn.responseCode == 405
    }

    // ---- customizer ------------------------------------------------------

    void "customizer Consumer is invoked before start and can register additional contexts"() {
        given:
        server.close()
        boolean customizerCalled = false

        when:
        server = new MiniProfilerServer(provider, { httpServer ->
            customizerCalled = true
            httpServer.createContext('/custom') { exchange ->
                MiniProfilerServer.sendResponse(exchange, 200, 'text/plain; charset=utf-8', 'hello'.bytes)
            }
        })

        then:
        customizerCalled

        and:
        def conn = connect('custom')
        conn.responseCode == 200
        conn.inputStream.text == 'hello'
    }
}
