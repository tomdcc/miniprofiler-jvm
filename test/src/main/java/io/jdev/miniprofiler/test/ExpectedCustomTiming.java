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

/**
 * Describes expected properties of a custom timing (e.g. a SQL query) for use
 * in test assertions. Instances are built via {@link ProfilerDsl}.
 *
 * <p>When comparing against actual custom timings, {@code commandString} is matched
 * with whitespace normalised (runs of whitespace collapsed to a single space,
 * leading/trailing whitespace trimmed).</p>
 */
public final class ExpectedCustomTiming {

    private final String executeType;
    private final String commandString;
    private final long minDurationMs;

    ExpectedCustomTiming(String executeType, String commandString, long minDurationMs) {
        this.executeType = executeType;
        this.commandString = commandString;
        this.minDurationMs = minDurationMs;
    }

    public String getExecuteType() {
        return executeType;
    }

    public String getCommandString() {
        return commandString;
    }

    public long getMinDurationMs() {
        return minDurationMs;
    }
}
