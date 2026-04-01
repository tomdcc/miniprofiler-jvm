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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Describes expected properties of a profiling timing step for use in test assertions.
 * Instances are built via {@link ProfilerDsl}.
 */
public final class ExpectedTiming {

    private final String name;
    private final long minDurationMs;
    private final List<ExpectedTiming> children;
    private final Map<String, List<ExpectedCustomTiming>> customTimings;

    ExpectedTiming(String name, long minDurationMs,
                   List<ExpectedTiming> children,
                   Map<String, List<ExpectedCustomTiming>> customTimings) {
        this.name = name;
        this.minDurationMs = minDurationMs;
        this.children = Collections.unmodifiableList(children);
        this.customTimings = Collections.unmodifiableMap(customTimings);
    }

    public String getName() {
        return name;
    }

    public long getMinDurationMs() {
        return minDurationMs;
    }

    /** Child timings in order. Never null; may be empty. */
    public List<ExpectedTiming> getChildren() {
        return children;
    }

    /**
     * Custom timings keyed by type (e.g. {@code "sql"}).
     * Never null; may be empty.
     */
    public Map<String, List<ExpectedCustomTiming>> getCustomTimings() {
        return customTimings;
    }
}
