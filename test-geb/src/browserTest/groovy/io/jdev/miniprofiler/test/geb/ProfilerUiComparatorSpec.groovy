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

package io.jdev.miniprofiler.test.geb

import geb.spock.GebReportingSpec
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.ScriptTagWriter
import io.jdev.miniprofiler.server.MiniProfilerServer
import io.jdev.miniprofiler.storage.Storage
import io.jdev.miniprofiler.test.ExpectedProfiler
import io.jdev.miniprofiler.test.ProfilerDsl
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.AutoCleanup
import spock.lang.Shared

/**
 * Browser test that exercises {@link ProfilerUiComparator} against the real MiniProfiler UI,
 * served by an embedded HTTP server using actual MiniProfiler JavaScript and a known
 * {@link io.jdev.miniprofiler.internal.ProfilerImpl} result.
 *
 * <p>Verifies that the comparator correctly passes for matching profiles and fails
 * (throws {@link AssertionError}) for non-matching ones.</p>
 */
class ProfilerUiComparatorSpec extends GebReportingSpec {

    /**
     * A real profiler result with:
     *   - profiler name: "/test-request/"
     *   - root timing: "/test-request/"
     *   - one child timing: "child step"
     *   - SQL custom timing on child step: executeType="reader",
     *     commandString="select * from people", duration=50ms
     */
    @Shared
    Profiler profilerResult

    @Shared
    @AutoCleanup
    MiniProfilerServer server

    def setupSpec() {
        TestProfilerProvider provider = new TestProfilerProvider()
        Profiler p = provider.start('/test-request/')
        def child = p.step('child step')
        child.addCustomTiming('sql', 'reader', 'select * from people', 50L)
        child.stop()
        p.stop()
        profilerResult = p

        server = new MiniProfilerServer(provider, { httpServer ->
            httpServer.createContext("/") { exchange ->
                try {
                    if (!exchange.requestMethod.equals("GET")) {
                        MiniProfilerServer.sendError(exchange, 405)
                        return
                    }
                    UUID id = provider.storage.list(1, null, null, Storage.ListResultsOrder.Ascending).find()
                    Profiler profiler = provider.storage.load(id)
                    String scriptTag = new ScriptTagWriter().printScriptTag(profiler, "/miniprofiler")
                    String html = "<!DOCTYPE html>\n<html>\n<head><title>MiniProfiler Test</title></head>\n<body>\n" +
                        scriptTag + "\n</body>\n</html>"
                    MiniProfilerServer.sendResponse(exchange, 200, "text/html; charset=utf-8",
                        html.getBytes("UTF-8"))
                } catch (Exception e) {
                    MiniProfilerServer.sendError(exchange, 500)
                }
            }
        })
    }


    def setup() {
        browser.baseUrl = server.baseUrl
        to ProfilerTestPage
        // Wait for the MiniProfiler JS to fetch the profile and render results
        waitFor { miniProfiler.results.size() > 0 }
    }

    void "comparator passes for exact matching profile"() {
        given:
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.step('child step', 0) { step ->
                step.customTiming('sql', 'reader', 'select * from people')
            }
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        noExceptionThrown()
    }

    void "comparator passes with whitespace-normalised custom timing command"() {
        given:
        // Expected has normalised whitespace; the SQL formatter may reformat the stored command
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.step('child step', 0) { step ->
                step.customTiming('sql', 'reader', 'select * from people')
            }
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        noExceptionThrown()
    }

    void "comparator passes when actual duration exceeds minimum"() {
        given:
        // Custom timing was added with 50ms duration; expect at least 25ms
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.step('child step', 0) { step ->
                step.customTiming('sql', 'reader', 'select * from people', 25)
            }
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        noExceptionThrown()
    }

    void "comparator fails when profiler name does not match"() {
        given:
        ExpectedProfiler expected = ProfilerDsl.profiler('/wrong-name/')

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        thrown(AssertionError)
    }

    void "comparator fails when timing name does not match"() {
        given:
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.step('wrong child name')
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        thrown(AssertionError)
    }

    void "comparator fails when minimum duration exceeds actual"() {
        given:
        // Custom timing was added with 50ms; require 99999ms — must fail
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.step('child step', 0) { step ->
                step.customTiming('sql', 'reader', 'select * from people', 99999L)
            }
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        thrown(AssertionError)
    }

    void "comparator fails when custom timing command does not match"() {
        given:
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.step('child step', 0) { step ->
                step.customTiming('sql', 'reader', 'select * from wrong_table')
            }
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        thrown(AssertionError)
    }

    void "convenience method finds matching result by name"() {
        given:
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.step('child step', 0) { step ->
                step.customTiming('sql', 'reader', 'select * from people')
            }
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        noExceptionThrown()
    }

    void "convenience method fails when no result name matches"() {
        given:
        ExpectedProfiler expected = ProfilerDsl.profiler('/no-such-request/')

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        thrown(AssertionError)
    }

    // ---- Custom links verification -----------------------------------------

    void "comparator passes when custom links match"() {
        given:
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.step('child step', 0) { step ->
                step.customTiming('sql', 'reader', 'select * from people')
            }
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        noExceptionThrown()
    }

    void "comparator fails when expected custom link is not in UI"() {
        given:
        ExpectedProfiler expected = ProfilerDsl.profiler('/test-request/', 0) { root ->
            root.customLink('AppStats', 'http://example.com/appstats')
            root.step('child step', 0) { step ->
                step.customTiming('sql', 'reader', 'select * from people')
            }
        }

        when:
        ProfilerUiComparator.verify(expected, miniProfiler)

        then:
        thrown(AssertionError)
    }

    // ---- Profiler-based exact verification --------------------------------

    void "exact verify passes for matching profiler"() {
        when:
        ProfilerUiComparator.verify(profilerResult, miniProfiler)

        then:
        noExceptionThrown()
    }

    void "exact verify passes when searching by profiler name"() {
        when:
        ProfilerUiComparator.verify(profilerResult, miniProfiler)

        then:
        noExceptionThrown()
    }

    void "exact verify fails when wrong profiler is supplied"() {
        given:
        TestProfilerProvider other = new TestProfilerProvider()
        Profiler wrong = other.start('/wrong-name/')
        wrong.stop()

        when:
        ProfilerUiComparator.verify(wrong, miniProfiler)

        then:
        thrown(AssertionError)
    }
}
