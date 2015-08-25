/*
 * Copyright 2013 the original author or authors.
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



package io.jdev.miniprofiler.intercept

import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class ProfilingInvocationHandlerSpec extends Specification {

	ProfilerProvider profilerProvider
	Profiler profiler
	TestInterface target
	TestInterface proxy

	void setup() {
		profilerProvider = new TestProfilerProvider()
		profiler = profilerProvider.start("ProfilingInvocationHandlerSpec")
		target = Mock(TestInterface)
		proxy = ProfilingInvocationHandler.createProxy(profilerProvider, target, TestInterface)
	}

	def "handler intercepts method call"() {
		when: 'call method'
			def result = proxy.method('hi there')

		then: 'target was called'
			1 * target.method('hi there') >> 'foobar'

		then: 'profiler was called'
			profiler.root.children.size() == 1
			profiler.root.children[0].name == 'TestInterface.method'

		and: 'result returned'
			result == 'foobar'

	}

	def "handler intercepts method call that throws exception"() {
		when: 'call method'
			proxy.method('hi there')

		then: 'target was called and threw exception'
			1 * target.method('hi there') >> { throw new RuntimeException('so sad')}

		then: 'profiler was called'
			profiler.root.children.size() == 1
			profiler.root.children[0].name == 'TestInterface.method'

		and: 'exception thrown'
			def e = thrown(Exception)
			while(e.cause) {
				e = e.cause
			}
			e.message == 'so sad'

	}

}
