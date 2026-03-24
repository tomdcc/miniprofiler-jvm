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


class MiniProfilerViewerServerSpec extends Specification {

    MiniProfilerViewerServer server

    void setup() {
        def storage = MiniProfilerViewerSingleFileStorage.forFile(ViewerTestFixtures.PROFILE_FILE.toPath())
        server = new MiniProfilerViewerServer(storage)
    }

    void cleanup() {
        server?.close()
    }

    private HttpURLConnection connect(String path, String accept = null, String method = 'GET', String body = null) {
        HttpURLConnection conn = new URL("http://localhost:${server.port}/${path}").openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        if (accept) { conn.setRequestProperty('Accept', accept) }
        if (method != 'GET') { conn.requestMethod = method }
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.withWriter('UTF-8') { it.write(body) }
        }
        conn
    }

    void "GET results returns HTML"() {
        when:
        def conn = connect("miniprofiler/results?id=${ViewerTestFixtures.PROFILE_ID}")

        then:
        conn.responseCode == 200
        conn.contentType.contains('text/html')
        conn.inputStream.text.contains('<html>')
    }

    void "GET results with JSON accept returns profiler JSON"() {
        when:
        def conn = connect("miniprofiler/results?id=${ViewerTestFixtures.PROFILE_ID}", 'application/json')
        def json = new JsonSlurper().parse(conn.inputStream)

        then:
        conn.responseCode == 200
        conn.contentType.contains('application/json')
        json.Id == ViewerTestFixtures.PROFILE_ID.toString()
        json.Name == '/test-request'
    }

    void "POST results with JSON body returns profiler JSON"() {
        when:
        def conn = connect('miniprofiler/results', 'application/json', 'POST', """{"Id": "${ViewerTestFixtures.PROFILE_ID}"}""")
        def json = new JsonSlurper().parse(conn.inputStream)

        then:
        conn.responseCode == 200
        conn.contentType.contains('application/json')
        json.Id == ViewerTestFixtures.PROFILE_ID.toString()
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

    void "GET static resource returns content"() {
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
}
