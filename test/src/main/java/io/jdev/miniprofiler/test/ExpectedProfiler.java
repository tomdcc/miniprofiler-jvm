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

import java.util.Map;

/**
 * Describes expected properties of a profiling session for use in test assertions.
 * Instances are built via {@link ProfilerDsl}.
 */
public final class ExpectedProfiler {

    private final String name;
    private final ExpectedTiming root;
    private final Map<String, String> customLinks;

    ExpectedProfiler(String name, ExpectedTiming root, Map<String, String> customLinks) {
        this.name = name;
        this.root = root;
        this.customLinks = customLinks;
    }

    /**
     * Returns the expected profiling session name.
     *
     * @return the expected name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the expected root timing step.
     *
     * @return the expected root timing
     */
    public ExpectedTiming getRoot() {
        return root;
    }

    /**
     * Returns the expected custom links, or {@code null} if none are expected.
     *
     * @return a map of link text to URL, or {@code null}
     */
    public Map<String, String> getCustomLinks() {
        return customLinks;
    }
}
