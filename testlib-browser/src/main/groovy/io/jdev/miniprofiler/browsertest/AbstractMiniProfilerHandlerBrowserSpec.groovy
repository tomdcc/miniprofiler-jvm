/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdev.miniprofiler.browsertest

import geb.spock.GebReportingSpec
import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.integtest.InProcessTestedServer
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.test.geb.MiniProfilerModule
import io.jdev.miniprofiler.test.geb.MiniProfilerPopupModule
import io.jdev.miniprofiler.test.geb.MiniProfilerQueriesPopupModule
import io.jdev.miniprofiler.test.geb.MiniProfilerQueryModule
import io.jdev.miniprofiler.test.geb.MiniProfilerResultsIndexPage
import io.jdev.miniprofiler.test.geb.MiniProfilerSingleResultPage
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared

/**
 * Abstract base class for in-process browser tests that verify a MiniProfiler handler
 * implementation serves the UI correctly.
 *
 * <p>Profiles are injected directly into storage rather than triggering profiling
 * via real HTTP requests, keeping the tests focused on the handler/UI interaction.
 * Subclasses implement {@link #createServer} to start the specific handler under test,
 * which owns its appropriate production {@link io.jdev.miniprofiler.ProfilerProvider}.</p>
 */
abstract class AbstractMiniProfilerHandlerBrowserSpec extends GebReportingSpec {

    @Shared
    @AutoCleanup
    InProcessTestedServer server

    /**
     * Subclasses implement this to start the specific handler implementation under test.
     * The server must be ready to accept requests when this method returns.
     */
    abstract protected InProcessTestedServer createServer()

    def setupSpec() {
        server = createServer()
    }

    def setup() {
        browser.baseUrl = server.serverUrl
    }

    def cleanup() {
        server.clearProfiles()
    }

    /**
     * Injects a profile with a child step and SQL custom timing directly into the server's
     * storage, bypassing the provider's threading model. Returns the saved profile's UUID.
     */
    protected UUID injectProfile(String name = '/test-request') {
        def p = new ProfilerImpl(name, ProfileLevel.Info, server.profilerProvider)
        def child = p.step('child step')
        child.addCustomTiming('sql', 'reader', 'select * from people', 50L)
        child.stop()
        server.addProfile(p)
        return p.id
    }

    void 'results page renders an injected profile'() {
        given:
        UUID id = injectProfile()

        when:
        go "miniprofiler/results?id=${id}"

        then:
        at MiniProfilerSingleResultPage
        waitFor { page.items.size() >= 1 }
    }

    void 'results page title matches profiling results pattern'() {
        given:
        UUID id = injectProfile()

        when:
        go "miniprofiler/results?id=${id}"

        then:
        at MiniProfilerSingleResultPage
        driver.title.startsWith('/test-request')
        driver.title.endsWith('- Profiling Results')
    }

    void 'results page popup shows timing rows with correct labels and hidden optional columns'() {
        given:
        UUID id = injectProfile()

        when:
        go "miniprofiler/results?id=${id}"

        then:
        at MiniProfilerSingleResultPage
        waitFor { page.items.size() >= 1 }
        def timings = (page.items[0] as MiniProfilerPopupModule).timings
        timings.size() == 2
        timings[0].label == '/test-request'
        timings[0].duration.text() ==~ /\d+\.\d+/
        !timings[0].durationWithChildren.displayed
        !timings[0].timeFromStart.displayed
        timings[1].label == 'child step'
        timings[1].duration.text() ==~ /\d+\.\d+/
    }

    void 'results page only child timing has queries link'() {
        given:
        UUID id = injectProfile()

        when:
        go "miniprofiler/results?id=${id}"

        then:
        at MiniProfilerSingleResultPage
        waitFor { page.items.size() >= 1 }
        def timings = (page.items[0] as MiniProfilerPopupModule).timings
        timings.size() == 2
        !timings[0].queries.displayed
        timings[1].queries.text() ==~ /\d+\.\d+ \(1\)/
    }

    void 'results page toggle child timings reveals duration-with-children and time-from-start'() {
        given:
        UUID id = injectProfile()

        when:
        go "miniprofiler/results?id=${id}"

        then:
        at MiniProfilerSingleResultPage
        waitFor { page.items.size() >= 1 }
        def popup = page.items[0] as MiniProfilerPopupModule
        def timings = popup.timings

        when:
        popup.toggleChildTimingLink.click()

        then:
        waitFor { timings[0].timeFromStart.displayed }
        timings[0].timeFromStart.text() ==~ /\+\d+\.\d+/
        timings[0].durationWithChildren.displayed
        timings[0].durationWithChildren.text() ==~ /\d+\.\d+/
        timings[1].timeFromStart.text() ==~ /\+\d+\.\d+/
        timings[1].durationWithChildren.displayed
    }

    void 'results page SQL queries section shows query with correct content'() {
        given:
        UUID id = injectProfile()

        when:
        go "miniprofiler/results?id=${id}"

        then:
        at MiniProfilerSingleResultPage
        waitFor { page.items.size() >= 1 }
        def timings = (page.items[0] as MiniProfilerPopupModule).timings

        when:
        timings[1].queries.click()

        then:
        waitFor { page.items.size() == 2 }
        def queries = (page.items[1] as MiniProfilerQueriesPopupModule).queries
        queries.size() >= 1
        and:
        def query = queries.find { it instanceof MiniProfilerQueryModule } as MiniProfilerQueryModule
        query != null
        query.step == 'child step'
        query.type == 'sql - reader'
        query.query ==~ /select\s+\*\s+from\s+people/
        query.duration ==~ /\d+\.\d+ ms \(T\+-?\d+\.\d+ ms\)/
    }

    void 'results-index page shows stored profiles'() {
        given:
        injectProfile('/request-one')
        injectProfile('/request-two')

        when:
        to MiniProfilerResultsIndexPage

        then:
        at MiniProfilerResultsIndexPage
        waitFor { page.resultRows.size() >= 2 }
    }

    void 'results-index page table is displayed and rows contain injected request names'() {
        given:
        injectProfile('/request-alpha')
        injectProfile('/request-beta')

        when:
        to MiniProfilerResultsIndexPage

        then:
        at MiniProfilerResultsIndexPage
        resultsTable.displayed
        waitFor { resultRows.size() >= 2 }
        def rowTexts = resultRows*.text()
        rowTexts.any { it.contains('/request-alpha') }
        rowTexts.any { it.contains('/request-beta') }
    }

    @Requires({ instance.server.profiledPagePath })
    void 'widget shows profiler button with timing on profiled page'() {
        when:
        go server.profiledPagePath

        then:
        waitFor { !module(MiniProfilerModule).results.empty }
        def mp = module(MiniProfilerModule)
        mp.results.size() == 1
        mp.results[0].button.time ==~ /\d+\.\d+ ms/
        !mp.results[0].popup.displayed
    }

    @Requires({ instance.server.profiledPagePath })
    void 'widget popup opens on button click and shows timing structure'() {
        when:
        go server.profiledPagePath

        then:
        waitFor { !module(MiniProfilerModule).results.empty }
        def result = module(MiniProfilerModule).results[0]

        when:
        result.button.click()

        then:
        waitFor { result.popup.displayed }
        def timings = result.popup.timings
        timings.size() == 2
        timings[0].label == '/test-page'
        timings[0].duration.text() ==~ /\d+\.\d+/
        !timings[0].durationWithChildren.displayed
        !timings[0].timeFromStart.displayed
        timings[1].label == 'child step'
        timings[1].duration.text() ==~ /\d+\.\d+/
        !timings[0].queries.displayed
        timings[1].queries.text() ==~ /\d+\.\d+ \(1\)/
    }

    @Requires({ instance.server.profiledPagePath })
    void 'widget popup toggle child timings reveals duration-with-children and time-from-start'() {
        when:
        go server.profiledPagePath

        then:
        waitFor { !module(MiniProfilerModule).results.empty }
        def result = module(MiniProfilerModule).results[0]

        when:
        result.button.click()

        then:
        waitFor { result.popup.displayed }
        def timings = result.popup.timings

        when:
        result.popup.toggleChildTimingLink.click()

        then:
        waitFor { timings[0].timeFromStart.displayed }
        timings[0].timeFromStart.text() ==~ /\+\d+\.\d+/
        timings[0].durationWithChildren.displayed
        timings[0].durationWithChildren.text() ==~ /\d+\.\d+/
        timings[1].timeFromStart.text() ==~ /\+\d+\.\d+/
        timings[1].durationWithChildren.displayed
    }

    @Requires({ instance.server.profiledPagePath })
    void 'widget popup queries link opens SQL queries overlay'() {
        when:
        go server.profiledPagePath

        then:
        waitFor { !module(MiniProfilerModule).results.empty }
        def mp = module(MiniProfilerModule)
        def result = mp.results[0]

        when:
        result.button.click()

        then:
        waitFor { result.popup.displayed }

        when:
        result.popup.timings[1].queries.click()

        then:
        waitFor { module(MiniProfilerModule).queriesPopup.displayed }
        def queries = module(MiniProfilerModule).queriesPopup.queries
        queries.size() >= 1
        and:
        def query = queries.find { it instanceof MiniProfilerQueryModule } as MiniProfilerQueryModule
        query != null
        query.step == 'child step'
        query.type == 'sql - reader'
        query.query ==~ /select\s+\*\s+from\s+people/
        query.duration ==~ /\d+\.\d+ ms \(T\+-?\d+\.\d+ ms\)/
    }

    @Requires({ instance.server.ajaxEndpointPath })
    void 'AJAX call adds a second result to the widget'() {
        when:
        go server.profiledPagePath

        then:
        waitFor { !module(MiniProfilerModule).results.empty }
        module(MiniProfilerModule).results.size() == 1

        when:
        $('#ajax-call').click()

        then:
        waitFor { module(MiniProfilerModule).results.size() == 2 }
        module(MiniProfilerModule).results[1].button.time ==~ /\d+\.\d+ ms/
    }

    @Requires({ instance.server.ajaxEndpointPath })
    void 'AJAX result popup shows correct timing and query structure'() {
        when:
        go server.profiledPagePath

        then:
        waitFor { !module(MiniProfilerModule).results.empty }

        when:
        $('#ajax-call').click()

        then:
        waitFor { module(MiniProfilerModule).results.size() == 2 }
        def result = module(MiniProfilerModule).results[1]

        when:
        result.button.click()

        then:
        waitFor { result.popup.displayed }
        def timings = result.popup.timings
        timings.size() == 2
        timings[1].label == 'ajax step'
        timings[1].queries.text() ==~ /\d+\.\d+ \(1\)/

        when:
        timings[1].queries.click()

        then:
        waitFor { module(MiniProfilerModule).queriesPopup.displayed }
        def query = module(MiniProfilerModule).queriesPopup.queries.find { it instanceof MiniProfilerQueryModule } as MiniProfilerQueryModule
        query != null
        query.step == 'ajax step'
        query.type == 'sql - reader'
        query.query ==~ /select\s+\*\s+from\s+ajax_data/
    }

    @Requires({ instance.server.profiledPagePath })
    void 'widget popup share link opens single result page'() {
        when:
        go server.profiledPagePath

        then:
        waitFor { !module(MiniProfilerModule).results.empty }
        def result = module(MiniProfilerModule).results[0]

        when:
        result.button.click()

        then:
        waitFor { result.popup.displayed }

        when:
        result.popup.shareLink.click()

        then:
        withWindow({ MiniProfilerSingleResultPage.matches(driver) }, page: MiniProfilerSingleResultPage) {
            waitFor { page.items.size() >= 1 }
        }
    }
}
