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

package io.jdev.miniprofiler.javaee

import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

import javax.interceptor.InvocationContext
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class ProfilingEJBInterceptorSpec extends Specification {

	TestProfilerProvider profilerProvider
	Profiler profiler
	Foo foo
	InvocationContext context
	ProfilingEJBInterceptor interceptor

	void setup() {
		profilerProvider = new TestProfilerProvider()
		profiler = profilerProvider.start("test")
		foo = new Foo()
		context = new MockInvocationContext()
		context.target = foo
		context.method = Comparable.getMethod("compareTo", Object)
		context.parameters = ["foo"] as Object[]
		interceptor = new ProfilingEJBInterceptor()
		interceptor.profilerProvider = profilerProvider
	}

	void "interceptor profiles target method"() {
		when: 'call interceptor'
		def result = interceptor.profile(context)

		then: 'returns correct result'
		result == 0

		and: 'was profiled'
		profiler.head.name == 'test'
		profiler.head.children.size() == 1
		profiler.head.children[0].name == 'Foo.compareTo'

		and: 'timing has been stopped'
		profiler.head.children[0].durationMilliseconds != null
	}

	void "interceptor profiles target method which throws exception"() {
		given: 'target will throw exception'
		context.error = new RuntimeException()

		when: 'call interceptor'
		interceptor.profile(context)

		then: 'exception thrown'
		def ex = thrown(RuntimeException)
		ex == context.error

		and: 'was profiled'
		profiler.head.name == 'test'
		profiler.head.children.size() == 1
		profiler.head.children[0].name == 'Foo.compareTo'

		and: 'timing has been stopped'
		profiler.head.children[0].durationMilliseconds != null
	}
}

class Foo implements Comparable<String> {
	@Override
	int compareTo(String o) {
		return 0
	}
}

class MockInvocationContext implements InvocationContext {
	Object target
	Object timer
	Method method
	Constructor<?> constructor
	Object[] parameters
	Map<String, Object> contextData
	Exception error
	Object proceed() throws Exception {
		if(error) {
			throw error
		}
		method.invoke(target, parameters);
	}
}