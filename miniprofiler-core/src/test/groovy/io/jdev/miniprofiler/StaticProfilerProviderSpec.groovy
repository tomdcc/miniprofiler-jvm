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

package io.jdev.miniprofiler

import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class StaticProfilerProviderSpec extends Specification {

	StaticProfilerProvider provider

	void setup() {
		provider = new StaticProfilerProvider()
	}

	void cleanup() {
		MiniProfiler.profilerProvider.stopCurrentSession(true)
		MiniProfiler.profilerProvider = null
	}

	void "provider delegates to static miniprofiler provider"() {
		given:
			MiniProfiler.profilerProvider = new TestProfilerProvider()

		when:
			def profiler = provider.start("foo")

		then:
			MiniProfiler.profilerProvider.currentProfiler == profiler
			MiniProfiler.profilerProvider.currentProfiler.head.name == 'foo'
	}

	void "provider delegates to static miniprofiler provider and uses created one when none set"() {
		// bit of an integration test, this
		given: 'no static profiler provider'
			MiniProfiler.profilerProvider = null

		when:
			def profiler = provider.start("foo")

		then: 'static profiler provider initialised'
			MiniProfiler.profilerProvider

		and: 'being used'
			MiniProfiler.profilerProvider.currentProfiler == profiler
			MiniProfiler.profilerProvider.currentProfiler.head.name == 'foo'
	}
}
