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

package io.jdev.miniprofiler.servlet

import geb.spock.GebReportingSpec
import io.jdev.miniprofiler.test.pages.MiniProfilerGapModule
import io.jdev.miniprofiler.test.pages.MiniProfilerQueryModule
import org.openqa.selenium.Dimension

class ServletMiniprofilerFunctionalSpec extends GebReportingSpec {

	void setup() {
		// ghostdriver way too small otherwise
		driver.manage().window().setSize(new Dimension(1024, 768))
	}

	void "can see miniprofiler"() {
		when:
			to HomePage

		then: 'mini profiler visible with single timing info'
			miniProfiler
			miniProfiler.results.size() == 1
			def result = miniProfiler.results[0]
			result.button.time ==~ ~/\d+\.\d ms/

		and: 'popup not visible'
			!result.popup.displayed

		when: 'click button'
			result.button.click()

		then: 'popup visible'
			result.popup.displayed

		and: ''
			def timings = result.popup.timings
			timings.size() == 1
			timings[0].label == '/miniprofiler-test-servlet/'
			timings[0].duration.text() ==~ ~/\d+\.\d/
			!timings[0].durationWithChildren.displayed
			!timings[0].timeFromStart.displayed
			timings[0].queries.text() ==~ ~/\d+\.\d \(1\)/

		when: 'toggle child timings'
			result.popup.toggleChildTimingLink.click()

		then: 'can see child timings column'
			waitFor { timings[0].timeFromStart.displayed }
			timings[0].timeFromStart.text() ==~ ~/\+\d+\.\d/
			timings[0].durationWithChildren.displayed
			timings[0].durationWithChildren.text() ==~ ~/\d+\.\d/

		when: 'click sql link'
			timings[0].queries.click()

		then: 'three timings, but trivial gaps not visible'
			def queries = result.queriesPopup.queries
			queries.size() == 3
			queries[0] instanceof MiniProfilerGapModule
			queries[0].displayed == !queries[0].trivial
			queries[1] instanceof MiniProfilerQueryModule
			queries[1].displayed
			queries[2] instanceof MiniProfilerGapModule
			queries[2].displayed == !queries[2].trivial

		and: 'query has correct info'
			queries[1].step == '/miniprofiler-test-servlet/'
			queries[1].timeFromStart ==~ ~/T\+\d+.\d ms/
			queries[1].duration ==~ ~/\d+.\d ms/
			queries[1].query ==~ ~/select\s+\*\s+from\s+people/
	}
}
