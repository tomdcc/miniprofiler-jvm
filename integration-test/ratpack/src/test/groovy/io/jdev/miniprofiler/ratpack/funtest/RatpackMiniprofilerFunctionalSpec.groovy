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

package io.jdev.miniprofiler.ratpack.funtest

import geb.spock.GebReportingSpec
import io.jdev.miniprofiler.test.pages.MiniProfilerGapModule
import io.jdev.miniprofiler.test.pages.MiniProfilerQueryModule
import io.jdev.miniprofiler.test.pages.MiniProfilerResultModule
import io.jdev.miniprofiler.test.pages.MiniProfilerModule
import io.jdev.miniprofiler.test.pages.MiniProfilerSingleResultPage
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.ServerBackedApplicationUnderTest

class RatpackMiniprofilerFunctionalSpec extends GebReportingSpec {

    ServerBackedApplicationUnderTest aut

    void setup() {
        aut = new MainClassApplicationUnderTest(Main)
        browser.baseUrl = aut.address.toString()

        // Go here first as there's an issue in the ui js the very first time it's loaded, related to something
        // being in localStorage I think. Basically the first miniprofiler load on the page is ok, but the second
        // one fails. All loads on subsequent pages are ok.
        to HomePage
        Thread.sleep(1000)

        // then reset - Geb tries to parse the about:blank and fails so we can't just go() there
        browser.js.exec('window.location = "about:blank"')
    }

    void "can see miniprofiler"() {
        when:
        to HomePage

        then: 'mini profiler visible with page timing info, plus ajax call eventually'
        miniProfiler
        waitFor(60) {
            miniProfiler.results.size() == 2
        }

        and:
        verifyResults(miniProfiler.results[0])
        verifyResults(miniProfiler.results[1])

        when:
        with(miniProfiler.results[0]) {
            button.click()
            assert waitFor { popup.displayed }
            popup.shareLink.click()
        }

        then:
        withWindow({ MiniProfilerSingleResultPage.matches(driver) }, page: MiniProfilerSingleResultPage) {
            assert driver.title ==~ /\/page .* - Profiling Results/
            waitFor {
                page.items.size() == 2
            }
        }

    }

    private void closeResultPopup(result) {
        if (result.popup.displayed) {
            miniProfiler.queriesPopup?.close()
            result.popup.close()
        }
    }

    private boolean verifyResults(MiniProfilerResultModule result) {
        assert result.button.time ==~ ~/\d+\.\d+ ms/

        assert !result.popup.displayed

        result.button.click()
        assert waitFor { result.popup.displayed }

        def timings = result.popup.timings
        assert timings.size() == 3
        assert timings.label == ['/page', 'TestHandler.handle', 'TestHandler.getData']
        assert timings[0].duration.text() ==~ ~/\d+\.\d+/
        assert !timings[0].durationWithChildren.displayed
        assert !timings[0].timeFromStart.displayed
        assert !timings[0].queries
        assert timings[2].queries.text() ==~ ~/\d+\.\d+ \(1\)/

        result.popup.toggleChildTimingLink.click()

        assert waitFor { timings[0].timeFromStart.displayed }
        assert timings[0].timeFromStart.text() ==~ ~/\+\d+\.\d+/
        assert timings[0].durationWithChildren.displayed
        assert timings[0].durationWithChildren.text() ==~ ~/\d+\.\d+/

        timings[2].queries.click()

        waitFor {
            miniProfiler.queriesPopup.queries.size() == 3
        }
        def queries = miniProfiler.queriesPopup.queries
        assert queries[0] instanceof MiniProfilerGapModule
        assert !queries[0].trivial

        assert queries[1] instanceof MiniProfilerQueryModule
        assert queries[1].type == 'sql - query'
        assert queries[1].step == 'TestHandler.getData'
        assert queries[1].duration ==~ ~/\d+\.\d+ ms \(T\+\d+\.\d+ ms\)/
        assert queries[1].query ==~ ~/select\s+\*\s+from\s+people/

        assert queries[2] instanceof MiniProfilerGapModule

        closeResultPopup(result)

        true
    }

    MiniProfilerModule getMiniProfiler() {
        page.miniProfiler as MiniProfilerModule
    }
}
