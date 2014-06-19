/*
 * Copyright 2014 the original author or authors.
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

package io.jdev.miniprofiler.servlet

import groovy.json.JsonSlurper
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.test.TestProfilerProvider
import io.jdev.miniprofiler.test.TestStorage
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

class ProfilingFilterSpec extends Specification {

	ProfilingFilter filter;
	MockFilterConfig config;
	MockServletContext context;
	FilterChain chain;
	MockHttpServletRequest request;
	MockHttpServletResponse response;
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
			request.addParameter("id", storage.profiler.id.toString())
			response = new MockHttpServletResponse()
			chain.invoked = false
			filter.doFilter(request, response, chain)

		then: 'chain not invoked'
			!chain.invoked

		and: 'serves up some json'
			println "response: $response.contentAsString"
			def json = new JsonSlurper().parseText(response.contentAsString)
			println json

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
			if(configuredResourcePath) {
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
			if(expectedResource) {
				def expectedBytes = Thread.currentThread().contextClassLoader.getResourceAsStream((expectedResource)).bytes
				expectedBytes == response.contentAsByteArray
			}

		where:
			configuredResourcePath | requestedResource           | chainCalled | expectedResource
            null                   | '/miniprofiler/README.md'   | false       | 'io/jdev/miniprofiler/ui/README.md'
            null                   | '/README.md'                | true        | null
			'/admin/miniprof'      | '/admin/miniprof/README.md' | false       | 'io/jdev/miniprofiler/ui/README.md'
			'/admin/miniprof'      | '/miniprofiler/README.md'   | true        | null
	}

}

class MockFilterChain implements FilterChain {

	ProfilerProvider profilerProvider
	boolean invoked = false

	@Override
	void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
		invoked = true
		profilerProvider.getCurrentProfiler().step("MockFilterChain").close();
	}
}
