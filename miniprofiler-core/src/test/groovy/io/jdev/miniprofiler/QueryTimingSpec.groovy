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

import spock.lang.Specification

class QueryTimingSpec extends Specification {

	void "calculates start milliseconds correctly"() {
		given:
			ProfilerImpl mp = new ProfilerImpl("hi there", ProfileLevel.Info, Mock(ProfilerProvider))
			TimingImpl timing = mp.getRoot()
			Thread.sleep(10)

		when: 'create query timing'
			def query = new CustomTiming("query", "select * from foo")

		and: 'ask for start'
			query.getStartMilliseconds()

		then: 'throws exception'
			thrown(IllegalStateException)

		when: 'set miniprofiler start'
			query.setParentTiming(timing)

		and: 'ask for start'
			long start = query.getStartMilliseconds()

		then: 'equals start time minus miniprofiler start'
			start == query.absoluteStartMilliseconds - mp.started

	}
}
