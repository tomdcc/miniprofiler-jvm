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

package io.jdev.miniprofiler.test;

import io.jdev.miniprofiler.CustomTiming;
import io.jdev.miniprofiler.Timing;
import io.jdev.miniprofiler.internal.ProfilerImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Verifies that an actual {@link ProfilerImpl} captured from profiled code matches
 * an expected profile built with {@link ProfilerDsl}.
 *
 * <p>Matching rules:</p>
 * <ul>
 *   <li>Profiler name must equal {@code expected.name}.</li>
 *   <li>For each timing (recursively): actual {@code durationMilliseconds >= expected.minDurationMs}.</li>
 *   <li>Children must be present in order and in the same count.</li>
 *   <li>For each custom timing type: lists must have the same count.</li>
 *   <li>Custom timing {@code commandString} comparison: whitespace normalised
 *       ({@code \s+} collapsed to a single space, trimmed). {@code executeType} matched exactly.</li>
 *   <li>Custom timing {@code durationMilliseconds >= expected.minDurationMs}.</li>
 * </ul>
 *
 * <p>Assertion failures throw {@link AssertionError} with a descriptive message.</p>
 */
public final class ProfilerComparator {

    private ProfilerComparator() {
    }

    /**
     * Verifies that {@code actual} matches {@code expected}.
     *
     * @param actual the profiler to verify
     * @param expected the expected profiler shape to verify against
     * @throws AssertionError if any aspect of the actual profiler does not match
     */
    public static void verify(ProfilerImpl actual, ExpectedProfiler expected) {
        assertEqual("profiler name", expected.getName(), actual.getName());
        verifyTiming(actual.getRoot(), expected.getRoot(), "root");
        verifyCustomLinks(actual.getCustomLinks(), expected.getCustomLinks());
    }

    private static void verifyCustomLinks(Map<String, String> actual, Map<String, String> expected) {
        Map<String, String> effectiveExpected = expected != null ? expected : Collections.emptyMap();
        Map<String, String> effectiveActual = actual != null ? actual : Collections.emptyMap();
        assertEqual("custom link count", effectiveExpected.size(), effectiveActual.size());
        for (Map.Entry<String, String> entry : effectiveExpected.entrySet()) {
            String text = entry.getKey();
            String expectedUrl = entry.getValue();
            String actualUrl = effectiveActual.get(text);
            if (actualUrl == null) {
                throw new AssertionError("Expected custom link '" + text + "' but none found");
            }
            assertEqual("custom link URL for '" + text + "'", expectedUrl, actualUrl);
        }
    }

    private static void verifyTiming(Timing actual, ExpectedTiming expected, String path) {
        assertEqual("timing name at " + path, expected.getName(), actual.getName());

        long actualDuration = actual.getDurationMilliseconds() != null ? actual.getDurationMilliseconds() : 0L;
        assertAtLeast("duration at " + path, expected.getMinDurationMs(), actualDuration);

        List<Timing> actualChildren = actual.getAllChildren();
        if (actualChildren == null) {
            actualChildren = Collections.emptyList();
        }
        List<ExpectedTiming> expectedChildren = expected.getChildren();
        assertEqual("child count at " + path, expectedChildren.size(), actualChildren.size());
        for (int i = 0; i < expectedChildren.size(); i++) {
            verifyTiming(actualChildren.get(i), expectedChildren.get(i), path + ".children[" + i + "]");
        }

        Map<String, List<CustomTiming>> actualCustomTimings = actual.getCustomTimings();
        if (actualCustomTimings == null) {
            actualCustomTimings = Collections.emptyMap();
        }
        Map<String, List<ExpectedCustomTiming>> expectedCustomTimings = expected.getCustomTimings();
        assertEqual("custom timing type count at " + path,
            expectedCustomTimings.size(), actualCustomTimings.size());
        for (Map.Entry<String, List<ExpectedCustomTiming>> entry : expectedCustomTimings.entrySet()) {
            String type = entry.getKey();
            List<ExpectedCustomTiming> expectedList = entry.getValue();
            List<CustomTiming> actualList = actualCustomTimings.get(type);
            if (actualList == null) {
                throw new AssertionError("Expected custom timings of type '" + type
                    + "' at " + path + " but none found");
            }
            assertEqual("custom timing count for type '" + type + "' at " + path,
                expectedList.size(), actualList.size());
            for (int i = 0; i < expectedList.size(); i++) {
                verifyCustomTiming(actualList.get(i), expectedList.get(i),
                    path + ".customTimings[" + type + "][" + i + "]");
            }
        }
    }

    private static void verifyCustomTiming(CustomTiming actual, ExpectedCustomTiming expected, String path) {
        assertEqual("executeType at " + path, expected.getExecuteType(), actual.getExecuteType());
        assertEqual("commandString at " + path,
            normalizeWhitespace(expected.getCommandString()),
            normalizeWhitespace(actual.getCommandString()));
        long actualDuration = actual.getDurationMilliseconds() != null ? actual.getDurationMilliseconds() : 0L;
        assertAtLeast("duration at " + path, expected.getMinDurationMs(), actualDuration);
    }

    private static String normalizeWhitespace(String s) {
        if (s == null) {
            return null;
        }
        return s.trim().replaceAll("\\s+", " ");
    }

    private static void assertEqual(String label, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertAtLeast(String label, long minExpected, long actual) {
        if (actual < minExpected) {
            throw new AssertionError(label + ": expected at least <" + minExpected + "> ms but was <" + actual + "> ms");
        }
    }
}
