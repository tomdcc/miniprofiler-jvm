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

    @Shared
    TestMiniProfilerHttpClient client

    /**
     * Subclasses implement this to start the specific handler implementation under test.
     * The server must be ready to accept requests when this method returns.
     */
    abstract protected InProcessTestedServer createServer()

    def setupSpec() {
        server = createServer()
        client = new TestMiniProfilerHttpClient(server.serverUrl)
    }

    def setup() {
        server.clearProfiles()
    }

    @Requires({ instance.server.profiledPagePath })
    void 'profiled request returns X-MiniProfiler-Ids header'() {
        when:
        def response = client.get(server.profiledPagePath)

        then:
        response.statusCode() == 200
        def id = response.miniProfilerId()
        UUID.fromString(id) != null
    }

    @Requires({ instance.server.profiledPagePath })
    void 'profiled request JSON results contain correct timing structure'() {
        when:
        def pageResponse = client.get(server.profiledPagePath)
        def resultResponse = client.getResultsJson(pageResponse.miniProfilerId())
        def profiler = resultResponse.bodyAsJson()

        then:
        resultResponse.statusCode() == 200
        resultResponse.contentType().orElse('').contains('application/json')
        profiler.Name != null
        profiler.Root != null
        profiler.Root.Children != null
        profiler.Root.Children.size() == 1
        profiler.Root.Children[0].CustomTimings?.sql?.size() == 1
    }

    void 'results endpoint returns HTML for known id'() {
        given:
        def profiler = server.createProfile('test-request')

        when:
        def response = client.getResultsHtml(profiler.id.toString())

        then:
        response.statusCode() == 200
        response.contentType().orElse('').contains('text/html')
        response.body().contains('<html>')
    }

    void 'results endpoint returns 404 for unknown id'() {
        when:
        def response = client.getResultsHtml(UUID.randomUUID().toString())

        then:
        response.statusCode() == 404
    }

    void 'results-list returns JSON array of stored profiles'() {
        given:
        def profiler = server.createProfile('test-request')

        when:
        def response = client.getResultsList()
        def results = response.bodyAsJson() as List

        then:
        response.statusCode() == 200
        results.size() == 1
        results[0].Id == profiler.id.toString()
        results[0].Name == 'test-request'
    }

    void 'results-list pagination with last-id filters results'() {
        given:
        def first = server.createProfile('first-request')
        Thread.sleep(10)
        def second = server.createProfile('second-request')

        when:
        def response = client.getResultsList(first.id.toString())
        def results = response.bodyAsJson() as List

        then:
        response.statusCode() == 200
        results.size() == 1
        results[0].Id == second.id.toString()
    }

    void 'results-index returns HTML'() {
        when:
        def response = client.getResultsIndex()

        then:
        response.statusCode() == 200
        response.contentType().orElse('').contains('text/html')
    }

    void 'static resource is served'() {
        when:
        def response = client.getStaticResource('includes.min.js')

        then:
        response.statusCode() == 200
        response.contentType().orElse('').contains('javascript')
        !response.body().empty
    }

    void 'unknown static resource returns 404'() {
        when:
        def response = client.getStaticResource('nonexistent.js')

        then:
        response.statusCode() == 404
    }

    @Requires({ instance.server.testUser && instance.server.profiledPagePath })
    void 'unviewed id from previous request appears in X-MiniProfiler-Ids header of next request'() {
        when: 'first request is made but results are not fetched'
        def firstResponse = client.get(server.profiledPagePath)
        def firstId = firstResponse.miniProfilerId()

        and: 'wait for async save if needed'
        server.waitForProfilerSave(UUID.fromString(firstId))

        and: 'second request is made'
        def secondResponse = client.get(server.profiledPagePath)

        then: 'the second response header includes the first request id as unviewed'
        secondResponse.miniProfilerIds().contains(firstId)
    }

    @Requires({ instance.server.testUser && instance.server.profiledPagePath })
    void 'after a profiled request the profiler id appears in getUnviewedIds for the test user'() {
        when:
        def response = client.get(server.profiledPagePath)
        def id = UUID.fromString(response.miniProfilerId())
        server.waitForProfilerSave(id)

        then:
        response.statusCode() == 200
        (server.profilerProvider.storage as io.jdev.miniprofiler.storage.MapStorage)
            .getUnviewedIds(server.testUser).contains(id)
    }

    @Requires({ instance.server.testUser && instance.server.profiledPagePath })
    void 'fetching results removes the profiler id from getUnviewedIds'() {
        given:
        def pageResponse = client.get(server.profiledPagePath)
        def id = pageResponse.miniProfilerId()
        server.waitForProfilerSave(UUID.fromString(id))
        assert (server.profilerProvider.storage as io.jdev.miniprofiler.storage.MapStorage)
            .getUnviewedIds(server.testUser).contains(UUID.fromString(id))

        when:
        client.getResultsJson(id)

        then:
        !(server.profilerProvider.storage as io.jdev.miniprofiler.storage.MapStorage)
            .getUnviewedIds(server.testUser).contains(UUID.fromString(id))
    }

    void 'POST results with Performance array returns ClientTimings in response'() {
        given:
        def profiler = server.createProfile('test-request')
        def id = profiler.id.toString()
        def performanceJson = '[{"Name":"fetchStart","Start":0,"Duration":12},{"Name":"firstPaintTime","Start":380}]'

        when:
        def response = client.postResultsJson(id, performanceJson)
        def body = response.bodyAsJson()

        then:
        response.statusCode() == 200
        body.ClientTimings != null
        body.ClientTimings.Timings.size() == 2
        body.ClientTimings.Timings[0].Name == 'fetchStart'
        body.ClientTimings.Timings[0].Duration == 12
        body.ClientTimings.Timings[1].Name == 'firstPaintTime'
        body.ClientTimings.Timings[1].Duration == null
    }

    void 'POST results with Performance array persists ClientTimings on subsequent GET'() {
        given:
        def profiler = server.createProfile('test-request')
        def id = profiler.id.toString()
        def performanceJson = '[{"Name":"fetchStart","Start":0,"Duration":12}]'
        client.postResultsJson(id, performanceJson)

        when:
        def response = client.getResultsJson(id)
        def body = response.bodyAsJson()

        then:
        response.statusCode() == 200
        body.ClientTimings != null
        body.ClientTimings.Timings.size() == 1
        body.ClientTimings.Timings[0].Name == 'fetchStart'
    }

    @Requires({ instance.server.ajaxEndpointPath })
    void 'ajax endpoint is profiled with X-MiniProfiler-Ids header'() {
        when:
        def response = client.get(server.ajaxEndpointPath)

        then:
        response.statusCode() == 200
        response.miniProfilerIds().size() == 1

        when:
        def resultResponse = client.getResultsJson(response.miniProfilerId())
        def profiler = resultResponse.bodyAsJson()

        then:
        resultResponse.statusCode() == 200
        profiler.Root != null
    }
}
