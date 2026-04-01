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

import io.jdev.miniprofiler.CustomTiming
import io.jdev.miniprofiler.Profiler
import io.jdev.miniprofiler.Timing
import io.jdev.miniprofiler.test.ExpectedCustomTiming
import io.jdev.miniprofiler.test.ExpectedProfiler
import io.jdev.miniprofiler.test.ExpectedTiming
import org.openqa.selenium.By
import org.openqa.selenium.Keys

/**
 * Verifies that an {@link ExpectedProfiler} built with {@link io.jdev.miniprofiler.test.ProfilerDsl}
 * matches what is rendered in the MiniProfiler browser UI, navigated via Geb page modules.
 *
 * <p>Assertion failures throw {@link AssertionError} with a descriptive message.</p>
 */
class ProfilerUiComparator {

    /**
     * Verifies the given {@code result} panel against {@code expected}.
     *
     * <p>The result button will be clicked to open the popup if it is not already open.</p>
     *
     * @throws AssertionError if any aspect of the rendered profile does not match
     */
    static void verify(ExpectedProfiler expected, MiniProfilerResultModule result) {
        def popup = result.popup
        if (!popup.displayed) {
            result.button.click()
            popup.waitFor { popup.displayed }
        }
        verifyPopup(expected, popup, result)
    }

    /**
     * Searches all results in the given {@link MiniProfilerModule} for one whose popup name
     * matches {@code expected.name}, then verifies it.
     *
     * <p>For each result the button is clicked to open the popup. If the popup name does not
     * match, ESC is sent to close it and the next result is tried. Fails if no result matches.</p>
     *
     * <p>The {@code miniProfiler} parameter accepts a {@link MiniProfilerModule} or Geb page
     * content wrapping one (e.g. {@code TemplateDerivedPageContent}).</p>
     *
     * @throws AssertionError if no result's name matches {@code expected.name}
     */
    @SuppressWarnings('MethodParameterTypeRequired')
    static void verify(ExpectedProfiler expected, miniProfiler) {
        def results = miniProfiler.results
        for (MiniProfilerResultModule result : results) {
            result.button.click()
            def popup = result.popup
            popup.waitFor { popup.displayed }
            String popupName = popup.name.text()?.trim()
            if (expected.name == popupName) {
                verifyPopup(expected, popup, result)
                return
            }
            // Close popup and try next
            def body = popup.$(By.xpath('/html/body'))
            body.firstElement().sendKeys(Keys.ESCAPE)
            popup.waitFor { !popup.displayed }
        }
        throw new AssertionError("No MiniProfiler result found with name '${expected.name}'; " +
            "found ${results.size()} result(s)")
    }

    /**
     * Verifies the given {@code result} panel exactly against {@code profiler}.
     *
     * <p>Every aspect of the rendered profile — name, timing names, durations,
     * child counts, custom timing types, counts, commands, and durations — must
     * match the profiler object exactly. Durations are matched within ±1 ms to
     * account for display rounding.</p>
     *
     * <p>The result button will be clicked to open the popup if it is not already open.</p>
     *
     * @throws AssertionError if any aspect of the rendered profile does not match
     */
    static void verify(Profiler profiler, MiniProfilerResultModule result) {
        def popup = result.popup
        if (!popup.displayed) {
            result.button.click()
            popup.waitFor { popup.displayed }
        }
        verifyPopupExact(profiler, popup, result)
    }

    /**
     * Searches all results in the given {@link MiniProfilerModule} for one whose popup name
     * matches {@code profiler.name}, then verifies it exactly.
     *
     * <p>See {@link #verify(Profiler, MiniProfilerResultModule)} for the definition of
     * exact verification.</p>
     *
     * @throws AssertionError if no result's name matches {@code profiler.name}
     */
    @SuppressWarnings('MethodParameterTypeRequired')
    static void verify(Profiler profiler, miniProfiler) {
        def results = miniProfiler.results
        for (MiniProfilerResultModule result : results) {
            result.button.click()
            def popup = result.popup
            popup.waitFor { popup.displayed }
            String popupName = popup.name.text()?.trim()
            if (profiler.name == popupName) {
                verifyPopupExact(profiler, popup, result)
                return
            }
            // Close popup and try next
            def body = popup.$(By.xpath('/html/body'))
            body.firstElement().sendKeys(Keys.ESCAPE)
            popup.waitFor { !popup.displayed }
        }
        throw new AssertionError("No MiniProfiler result found with name '${profiler.name}'; " +
            "found ${results.size()} result(s)")
    }

    @SuppressWarnings('MethodParameterTypeRequired')
    private static void verifyPopupExact(Profiler profiler, popup, MiniProfilerResultModule result) {
        String popupName = popup.name.text()?.trim()
        assertEqual("profiler name", profiler.name, popupName)

        List<MiniProfilerTimingRowModule> rows = popup.timings
        int nextIndex = verifyTimingRowExact(rows, 0, profiler.root, result)
        if (nextIndex != rows.size()) {
            throw new AssertionError(
                "Expected ${nextIndex} timing row(s) in the UI but found ${rows.size()}")
        }
    }

    private static int verifyTimingRowExact(List<MiniProfilerTimingRowModule> rows, int rowIndex,
                                             Timing timing, MiniProfilerResultModule result) {
        if (rowIndex >= rows.size()) {
            throw new AssertionError(
                "Expected timing '${timing.name}' at row ${rowIndex} " +
                "but only ${rows.size()} row(s) present")
        }
        MiniProfilerTimingRowModule row = rows[rowIndex]
        String label = row.label?.trim()
        assertEqual("timing name at row ${rowIndex}", timing.name, label)

        if (timing.customTimings) {
            verifyCustomTimingsExact(timing, result, row, rowIndex)
        }

        int nextIndex = rowIndex + 1
        for (Timing child : (timing.children ?: [])) {
            nextIndex = verifyTimingRowExact(rows, nextIndex, child, result)
        }
        return nextIndex
    }

    private static void verifyCustomTimingsExact(Timing timing, MiniProfilerResultModule result,
                                                  MiniProfilerTimingRowModule row, int rowIndex) {
        if (!row.queries.displayed) {
            throw new AssertionError(
                "Expected custom timings for '${timing.name}' at row ${rowIndex} " +
                "but no queries link is displayed")
        }
        row.queries.click()

        def queriesPopup = result.parent().siblings('.mp-overlay')
            .module(MiniProfilerQueriesPopupModule)
        queriesPopup.waitFor { queriesPopup.displayed }

        List actualQueries = queriesPopup.queries.findAll { it instanceof MiniProfilerQueryModule }

        int expectedTotal = timing.customTimings.values().sum { it.size() } as int
        assertEqual("total custom timing count at row ${rowIndex}", expectedTotal, actualQueries.size())

        int queryIndex = 0
        for (Map.Entry<String, List<CustomTiming>> entry : timing.customTimings.entrySet()) {
            for (CustomTiming ct : entry.value) {
                MiniProfilerQueryModule actualQuery = actualQueries[queryIndex] as MiniProfilerQueryModule
                String path = "row ${rowIndex}, custom timing ${queryIndex}"

                String actualType = actualQuery.type?.trim()
                assertEqual("type at ${path}", "${entry.key} - ${ct.executeType}", actualType)

                String actualCommand = actualQuery.query?.trim()
                assertEqual("commandString at ${path}",
                    normalizeWhitespace(ct.commandString),
                    normalizeWhitespace(actualCommand))

                String durationText = actualQuery.duration?.trim()
                double actualDurationMs = parseDuration(durationText, path)
                assertWithinRounding("duration at ${path}", ct.durationMilliseconds, actualDurationMs)

                queryIndex++
            }
        }
    }

    @SuppressWarnings('MethodParameterTypeRequired')
    private static void verifyPopup(ExpectedProfiler expected, popup,
                                    MiniProfilerResultModule result) {
        String popupName = popup.name.text()?.trim()
        assertEqual("profiler name", expected.name, popupName)

        List<MiniProfilerTimingRowModule> rows = popup.timings
        verifyTimingRow(rows, 0, expected.root, result)
    }

    /**
     * Verifies the timing row at {@code rowIndex} against {@code expected}, then recurses into
     * children. Returns the index of the next row to process after this timing's subtree.
     */
    private static int verifyTimingRow(List<MiniProfilerTimingRowModule> rows, int rowIndex,
                                       ExpectedTiming expected, MiniProfilerResultModule result) {
        if (rowIndex >= rows.size()) {
            throw new AssertionError(
                "Expected timing '${expected.name}' at row ${rowIndex} " +
                "but only ${rows.size()} row(s) present")
        }
        MiniProfilerTimingRowModule row = rows[rowIndex]
        String label = row.label?.trim()
        assertEqual("timing name at row ${rowIndex}", expected.name, label)

        String durationText = row.duration.text()?.trim()
        double actualDurationMs = parseDuration(durationText, "row ${rowIndex}")
        assertAtLeast("duration at row ${rowIndex}", expected.minDurationMs as double, actualDurationMs)

        if (!expected.customTimings.isEmpty()) {
            verifyCustomTimingsFromUi(expected, result, row, rowIndex)
        }

        int nextIndex = rowIndex + 1
        for (ExpectedTiming child : expected.children) {
            nextIndex = verifyTimingRow(rows, nextIndex, child, result)
        }
        return nextIndex
    }

    private static void verifyCustomTimingsFromUi(ExpectedTiming expectedTiming,
                                                   MiniProfilerResultModule result,
                                                   MiniProfilerTimingRowModule row,
                                                   int rowIndex) {
        if (!row.queries.displayed) {
            throw new AssertionError(
                "Expected custom timings for '${expectedTiming.name}' at row ${rowIndex} " +
                "but no queries link is displayed")
        }
        row.queries.click()

        // The queries overlay is a sibling of .mp-results, which is the parent of .mp-result
        def queriesPopup = result.parent().siblings('.mp-overlay')
            .module(MiniProfilerQueriesPopupModule)
        queriesPopup.waitFor { queriesPopup.displayed }

        List actualQueries = queriesPopup.queries.findAll { it instanceof MiniProfilerQueryModule }

        int expectedTotal = expectedTiming.customTimings.values().sum { it.size() } as int
        assertEqual("total custom timing count at row ${rowIndex}", expectedTotal, actualQueries.size())

        int queryIndex = 0
        for (Map.Entry<String, List<ExpectedCustomTiming>> entry : expectedTiming.customTimings.entrySet()) {
            for (ExpectedCustomTiming expectedCt : entry.value) {
                MiniProfilerQueryModule actualQuery = actualQueries[queryIndex] as MiniProfilerQueryModule
                String path = "row ${rowIndex}, custom timing ${queryIndex}"

                String actualType = actualQuery.type?.trim()
                String expectedType = "${entry.key} - ${expectedCt.executeType}"
                assertEqual("type at ${path}", expectedType, actualType)

                String actualCommand = actualQuery.query?.trim()
                assertEqual("commandString at ${path}",
                    normalizeWhitespace(expectedCt.commandString),
                    normalizeWhitespace(actualCommand))

                String durationText = actualQuery.duration?.trim()
                double actualDurationMs = parseDuration(durationText, path)
                assertAtLeast("duration at ${path}", expectedCt.minDurationMs as double, actualDurationMs)

                queryIndex++
            }
        }
    }

    private static String normalizeWhitespace(String s) {
        s?.trim()?.replaceAll(/\s+/, ' ')
    }

    private static double parseDuration(String text, String context) {
        if (!text) {
            throw new AssertionError("Cannot parse duration at ${context}: text is null or empty")
        }
        def matcher = text =~ /(\d+(?:\.\d+)?)/
        if (!matcher) {
            throw new AssertionError("Cannot parse duration at ${context}: '${text}'")
        }
        return Double.parseDouble(matcher[0][1] as String)
    }

    private static void assertEqual(String label, Object expected, Object actual) {
        if (expected == null ? actual != null : expected != actual) {
            throw new AssertionError("${label}: expected <${expected}> but was <${actual}>")
        }
    }

    private static void assertAtLeast(String label, double minExpected, double actual) {
        if (actual < minExpected) {
            throw new AssertionError(
                "${label}: expected at least <${minExpected}> ms but was <${actual}> ms")
        }
    }

    private static void assertWithinRounding(String label, Long expectedMs, double actualMs) {
        if (expectedMs == null) {
            throw new AssertionError("${label}: profiler duration is null (timing not stopped?)")
        }
        if (Math.abs(actualMs - expectedMs) >= 1.0) {
            throw new AssertionError(
                "${label}: expected <${expectedMs}> ms but displayed <${actualMs}> ms")
        }
    }
}
