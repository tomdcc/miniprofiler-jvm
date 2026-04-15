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

package io.jdev.miniprofiler.jakarta.servlet

import groovy.json.JsonSlurper
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.storage.MapStorage
import io.jdev.miniprofiler.test.TestProfilerProvider
import io.jdev.miniprofiler.test.TestStorage
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.security.Principal

class ProfilingFilterSpec extends Specification {

    ProfilingFilter filter
    MockFilterConfig config
    MockServletContext context
    FilterChain chain
    MockHttpServletRequest request
    MockHttpServletResponse response
    TestProfilerProvider profilerProvider
    TestStorage storage

    void setup() {
        context = new MockServletContext()
        config = new MockFilterConfig(context)
        storage = new TestStorage()
        profilerProvider = new TestProfilerProvider(storage: storage)

        filter = new ProfilingFilter()
        filter.profilerProvider = profilerProvider
        filter.init(config)

        chain = new MockFilterChain(profilerProvider: profilerProvider)
        request = new MockHttpServletRequest()
        response = new MockHttpServletResponse()
    }


    void "passes normal requests through with profiler set up"() {
        given: 'request'
        request.requestURI = '/foo'

        when: 'invoked'
        filter.doFilter(request, response, chain)

        then: 'chain was invoked'
        chain.invoked

        and: 'profiling session was captured'
        storage.profiler

        and: 'profiler id set in header'
        response.getHeader("X-MiniProfiler-Ids") == """["$storage.profiler.id"]"""

        and: 'has expected values'
        storage.profiler.root.name == '/foo'
        storage.profiler.root.children.size() == 1
        storage.profiler.root.children[0].name == 'MockFilterChain'

        when: 'ask for json'
        request = new MockHttpServletRequest('GET', '/miniprofiler/results')
        request.content = "{\"Id\":\"${storage.profiler.id}\"}".getBytes(StandardCharsets.UTF_8)
        request.addHeader("Accept", "application/json")
        response = new MockHttpServletResponse()
        chain.invoked = false
        filter.doFilter(request, response, chain)

        then: 'chain not invoked'
        !chain.invoked

        and: 'serves up some json'
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)

        and: 'it looks about right'
        json.Id == storage.profiler.id.toString()
        json.Name == "/foo"
        json.Root.Name == '/foo'
        json.Root.Children.size() == 1
        json.Root.Children[0].Name == 'MockFilterChain'


    }

    @Unroll
    void "returns ui resource"() {
        given: 'configured filter'
        if (configuredResourcePath) {
            config.addInitParameter('path', configuredResourcePath)
            filter.init(config)
        }

        and: 'request'
        request.requestURI = requestedResource

        when: 'invoked'
        filter.doFilter(request, response, chain)

        then: 'chain was invoked if expected'
        chain.invoked == chainCalled

        and: 'returned expected resource'
        if (expectedResource) {
            def expectedBytes = Thread.currentThread().contextClassLoader.getResourceAsStream((expectedResource)).bytes
            expectedBytes == response.contentAsByteArray
        }

        where:
        configuredResourcePath | requestedResource             | chainCalled | expectedResource
        null                   | '/miniprofiler/includes.js'   | false       | 'io/jdev/miniprofiler/ui/includes.js'
        null                   | '/includes.js'                | true        | null
        '/admin/miniprof'      | '/admin/miniprof/includes.js' | false       | 'io/jdev/miniprofiler/ui/includes.js'
        '/admin/miniprof'      | '/miniprofiler/includes.js'   | true        | null
    }

    void "cors host header added to results and resources served"() {
        given: 'configured filter'
        def host = 'http://foo.com'
        config.addInitParameter('allowed-origin', host)
        filter.init(config)

        and: 'profiler'
        storage.profiler = new ProfilerImpl("test", ProfileLevel.Info, profilerProvider)

        when: 'ask for results'
        request.requestURI = '/miniprofiler/results'
        request.content = "{\"Id\":\"${storage.profiler.id}\"}".getBytes(StandardCharsets.UTF_8)
        request.addHeader("Accept", "application/json")
        filter.doFilter(request, response, chain)

        then: 'results have correct header'
        response.getHeader("Access-Control-Allow-Origin") == host

    }

    void "use specified id if passed as a parameter"() {
        given: 'request'
        request.requestURI = '/foo'
        def id = UUID.randomUUID().toString()
        request.addParameter("x-miniprofiler-id", id)

        when: 'invoked'
        filter.doFilter(request, response, chain)

        then: 'chain was invoked'
        chain.invoked

        and: 'profiling session was captured'
        storage.profiler

        and: 'profiler id is as passed in'
        storage.profiler.id.toString() == id

        and: 'profiler id set in header'
        response.getHeader("X-MiniProfiler-Ids") == """["$id"]"""
    }

    void "results-index returns HTML list page"() {
        given: 'request'
        request = new MockHttpServletRequest('GET', '/miniprofiler/results-index')

        when: 'invoked'
        filter.doFilter(request, response, chain)

        then: 'chain not invoked'
        !chain.invoked

        and: 'returns html'
        response.status == 200
        response.contentType.contains('text/html')

        and: 'contains list table'
        response.contentAsString.contains("mp-results-index")

        and: 'contains listInit call'
        response.contentAsString.contains("MiniProfiler.listInit")
    }

    void "results-list returns JSON array of stored profiles"() {
        given: 'map storage with profilers'
        def mapStorage = new MapStorage()
        profilerProvider.storage = mapStorage
        def p1 = new ProfilerImpl('test1', ProfileLevel.Info, profilerProvider)
        p1.stop()
        def p2 = new ProfilerImpl('test2', ProfileLevel.Info, profilerProvider)
        p2.stop()

        and: 'request'
        request = new MockHttpServletRequest('GET', '/miniprofiler/results-list')

        when: 'invoked'
        filter.doFilter(request, response, chain)

        then: 'chain not invoked'
        !chain.invoked

        and: 'returns json'
        response.status == 200
        response.contentType.contains('application/json')

        and: 'contains profiler data'
        def json = new JsonSlurper().parseText(response.contentAsString)
        json instanceof List
        json.size() == 2
    }

    void "results-list JSON does not contain Root"() {
        given: 'map storage with a profiler'
        def mapStorage = new MapStorage()
        profilerProvider.storage = mapStorage
        def p1 = new ProfilerImpl('test1', ProfileLevel.Info, profilerProvider)
        p1.stop()

        and: 'request'
        request = new MockHttpServletRequest('GET', '/miniprofiler/results-list')

        when: 'invoked'
        filter.doFilter(request, response, chain)

        then: 'returned JSON does not contain Root'
        def json = new JsonSlurper().parseText(response.contentAsString)
        !json[0].containsKey('Root')
    }

    void "results-list with last-id filters results"() {
        given: 'map storage with profilers at different times'
        def mapStorage = new MapStorage()
        profilerProvider.storage = mapStorage
        def p1 = new ProfilerImpl(null, 'test1', 'test1', ProfileLevel.Info, profilerProvider)
        mapStorage.save(p1)
        Thread.sleep(10)
        def p2 = new ProfilerImpl(null, 'test2', 'test2', ProfileLevel.Info, profilerProvider)
        mapStorage.save(p2)
        Thread.sleep(10)
        def p3 = new ProfilerImpl(null, 'test3', 'test3', ProfileLevel.Info, profilerProvider)
        mapStorage.save(p3)

        and: 'request with last-id set to p1'
        request = new MockHttpServletRequest('GET', '/miniprofiler/results-list')
        request.addParameter('last-id', p1.id.toString())

        when: 'invoked'
        filter.doFilter(request, response, chain)

        then: 'returns profiles started after p1'
        def json = new JsonSlurper().parseText(response.contentAsString)
        json instanceof List
        json.size() == 2
        json*.Id.contains(p2.id.toString())
        json*.Id.contains(p3.id.toString())
        !json*.Id.contains(p1.id.toString())
    }

    void "binds the request to ServletRequestHolder during chain invocation and clears it afterwards"() {
        given: 'request and a chain that captures the bound request'
        request.requestURI = '/foo'
        request.userPrincipal = { 'alice' } as Principal
        HttpServletRequest holderDuringChain = null
        def captured = new FilterChain() {
            @Override
            void doFilter(ServletRequest req, ServletResponse resp) throws IOException, ServletException {
                holderDuringChain = ServletRequestHolder.current()
                profilerProvider.current().step('MockFilterChain').close()
            }
        }

        when: 'invoked'
        filter.doFilter(request, response, captured)

        then: 'holder pointed at the request during the chain'
        holderDuringChain.is(request)

        and: 'holder is cleared once the filter returns'
        ServletRequestHolder.current() == null

        and: 'captured user is recorded on the profile'
        storage.profiler.user == 'alice'
    }

    void "clears ServletRequestHolder even when the chain throws"() {
        given:
        request.requestURI = '/boom'
        def boomChain = new FilterChain() {
            @Override
            void doFilter(ServletRequest req, ServletResponse resp) throws IOException, ServletException {
                throw new ServletException('boom')
            }
        }

        when:
        filter.doFilter(request, response, boomChain)

        then:
        thrown(ServletException)
        ServletRequestHolder.current() == null
    }

    void "serves standalone results"() {
        given: 'profiler'
        storage.profiler = new ProfilerImpl("test", ProfileLevel.Info, profilerProvider).tap {
            stop()
        }

        and: 'request'
        request.requestURI = '/miniprofiler/results'
        request.addParameter("id", storage.profiler.id.toString())

        when: 'invoked'
        filter.doFilter(request, response, chain)

        then: 'chain not invoked'
        !chain.invoked

        and: 'serves profiler json'
        def body = response.contentAsString
        body.contains(storage.profiler.asUiJson())

        and: 'serves includes script tag'
        body.contains("<script type='text/javascript' id='mini-profiler' src='/miniprofiler/includes.js?version=")
    }
}

class MockFilterChain implements FilterChain {

    ProfilerProvider profilerProvider
    boolean invoked = false

    @Override
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        invoked = true
        profilerProvider.current().step("MockFilterChain").close()
    }
}
