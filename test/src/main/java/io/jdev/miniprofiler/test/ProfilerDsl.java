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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DSL for building {@link ExpectedProfiler} instances to use in test assertions.
 *
 * <p>Designed to be called from Java, Groovy, or Kotlin. In Groovy and Kotlin the
 * {@link Consumer}-typed spec parameters can be supplied as closures/lambdas, enabling
 * natural DSL-style syntax:</p>
 *
 * <pre>
 * // Groovy
 * def expected = ProfilerDsl.profiler('/my-request') {
 *     step('do some work', 5) {
 *         customTiming 'sql', 'reader', 'select * from people', 1
 *     }
 * }
 *
 * // Kotlin
 * val expected = ProfilerDsl.profiler("/my-request") {
 *     step("do some work", 5) {
 *         customTiming("sql", "reader", "select * from people", 1)
 *     }
 * }
 * </pre>
 *
 * <p>The resulting {@link ExpectedProfiler} can be passed to
 * {@link ProfilerComparator#verify} to assert that an actual profiling session
 * matches the expected structure.</p>
 */
public final class ProfilerDsl {

    private ProfilerDsl() {
    }

    /**
     * Creates an {@link ExpectedProfiler} with the given name and minimum duration,
     * configured by the given spec which can set profiler-level properties such as custom links.
     *
     * <p>The root timing of the profiler will have the same name as the profiler,
     * matching the behaviour of {@link io.jdev.miniprofiler.internal.ProfilerImpl}.</p>
     *
     * @param name         the expected profiler name
     * @param minDurationMs minimum expected duration in milliseconds
     * @param spec         spec that configures the profiler's custom links and root timing
     * @return the built {@link ExpectedProfiler}
     */
    public static ExpectedProfiler profiler(String name, long minDurationMs, Consumer<ProfilerBuilder> spec) {
        ProfilerBuilder builder = new ProfilerBuilder(name, minDurationMs);
        spec.accept(builder);
        return builder.build();
    }

    /**
     * Creates an {@link ExpectedProfiler} with the given name and no minimum duration
     * constraint, configured by the given spec.
     *
     * @param name the expected profiler name
     * @param spec spec that configures the profiler's custom links and root timing
     * @return the built {@link ExpectedProfiler}
     */
    public static ExpectedProfiler profiler(String name, Consumer<ProfilerBuilder> spec) {
        return profiler(name, 0, spec);
    }

    /**
     * Creates an {@link ExpectedProfiler} with the given name and a minimum duration
     * but no children or custom timings.
     *
     * @param name the expected profiler name
     * @param minDurationMs minimum expected duration in milliseconds
     * @return the built {@link ExpectedProfiler}
     */
    public static ExpectedProfiler profiler(String name, long minDurationMs) {
        return profiler(name, minDurationMs, b -> {
        });
    }

    /**
     * Creates an {@link ExpectedProfiler} with the given name and no minimum duration,
     * children, or custom timings.
     *
     * @param name the expected profiler name
     * @return the built {@link ExpectedProfiler}
     */
    public static ExpectedProfiler profiler(String name) {
        return profiler(name, 0, b -> {
        });
    }

    /**
     * Builder for an {@link ExpectedTiming} within the DSL.
     */
    public static final class TimingBuilder {

        private final String name;
        private final long minDurationMs;
        private final List<ExpectedTiming> children = new ArrayList<>();
        private final Map<String, List<ExpectedCustomTiming>> customTimings = new LinkedHashMap<>();

        private TimingBuilder(String name, long minDurationMs) {
            this.name = name;
            this.minDurationMs = minDurationMs;
        }

        /**
         * Adds a child timing step with a minimum duration, configured by the given spec.
         *
         * @param stepName the name of the timing step
         * @param stepMinDurationMs minimum expected duration in milliseconds
         * @param spec spec that configures the step's children and custom timings
         * @return this builder
         */
        public TimingBuilder step(String stepName, long stepMinDurationMs, Consumer<TimingBuilder> spec) {
            TimingBuilder child = new TimingBuilder(stepName, stepMinDurationMs);
            spec.accept(child);
            children.add(child.build());
            return this;
        }

        /**
         * Adds a child timing step with no minimum duration constraint, configured by the given spec.
         *
         * @param stepName the name of the timing step
         * @param spec spec that configures the step's children and custom timings
         * @return this builder
         */
        public TimingBuilder step(String stepName, Consumer<TimingBuilder> spec) {
            return step(stepName, 0, spec);
        }

        /**
         * Adds a child timing step with no minimum duration and no children or custom timings.
         *
         * @param stepName the name of the timing step
         * @return this builder
         */
        public TimingBuilder step(String stepName) {
            return step(stepName, 0, b -> {
            });
        }

        /**
         * Adds an expected custom timing entry (e.g. a SQL query) with a minimum duration.
         *
         * @param type          the custom timing type key (e.g. {@code "sql"})
         * @param executeType   the execute type (e.g. {@code "reader"})
         * @param commandString the command string; whitespace will be normalised during comparison
         * @param minDurationMs minimum expected duration in milliseconds
         * @return this builder
         */
        public TimingBuilder customTiming(String type, String executeType, String commandString, long minDurationMs) {
            customTimings.computeIfAbsent(type, k -> new ArrayList<>())
                .add(new ExpectedCustomTiming(executeType, commandString, minDurationMs));
            return this;
        }

        /**
         * Adds an expected custom timing entry with no minimum duration constraint.
         *
         * @param type the custom timing type key (e.g. {@code "sql"})
         * @param executeType the execute type (e.g. {@code "reader"})
         * @param commandString the command string; whitespace will be normalised during comparison
         * @return this builder
         */
        public TimingBuilder customTiming(String type, String executeType, String commandString) {
            return customTiming(type, executeType, commandString, 0);
        }

        ExpectedTiming build() {
            return new ExpectedTiming(name, minDurationMs, children, customTimings);
        }
    }

    /**
     * Builder for an {@link ExpectedProfiler} within the DSL.
     *
     * <p>Wraps a {@link TimingBuilder} for the root timing and adds profiler-level
     * properties such as custom links. All timing methods delegate to the root builder.</p>
     */
    public static final class ProfilerBuilder {

        private final TimingBuilder rootBuilder;
        private final Map<String, String> customLinks = new LinkedHashMap<>();

        private ProfilerBuilder(String name, long minDurationMs) {
            this.rootBuilder = new TimingBuilder(name, minDurationMs);
        }

        /**
         * Adds a child timing step with a minimum duration, configured by the given spec.
         *
         * @param stepName the name of the timing step
         * @param stepMinDurationMs minimum expected duration in milliseconds
         * @param spec spec that configures the step's children and custom timings
         * @return this builder
         */
        public ProfilerBuilder step(String stepName, long stepMinDurationMs, Consumer<TimingBuilder> spec) {
            rootBuilder.step(stepName, stepMinDurationMs, spec);
            return this;
        }

        /**
         * Adds a child timing step with no minimum duration constraint, configured by the given spec.
         *
         * @param stepName the name of the timing step
         * @param spec spec that configures the step's children and custom timings
         * @return this builder
         */
        public ProfilerBuilder step(String stepName, Consumer<TimingBuilder> spec) {
            rootBuilder.step(stepName, spec);
            return this;
        }

        /**
         * Adds a child timing step with no minimum duration and no children or custom timings.
         *
         * @param stepName the name of the timing step
         * @return this builder
         */
        public ProfilerBuilder step(String stepName) {
            rootBuilder.step(stepName);
            return this;
        }

        /**
         * Adds an expected custom timing entry (e.g. a SQL query) with a minimum duration.
         *
         * @param type          the custom timing type key (e.g. {@code "sql"})
         * @param executeType   the execute type (e.g. {@code "reader"})
         * @param commandString the command string
         * @param minDurationMs minimum expected duration in milliseconds
         * @return this builder
         */
        public ProfilerBuilder customTiming(String type, String executeType, String commandString, long minDurationMs) {
            rootBuilder.customTiming(type, executeType, commandString, minDurationMs);
            return this;
        }

        /**
         * Adds an expected custom timing entry with no minimum duration constraint.
         *
         * @param type the custom timing type key (e.g. {@code "sql"})
         * @param executeType the execute type (e.g. {@code "reader"})
         * @param commandString the command string
         * @return this builder
         */
        public ProfilerBuilder customTiming(String type, String executeType, String commandString) {
            rootBuilder.customTiming(type, executeType, commandString);
            return this;
        }

        /**
         * Adds a custom link to the expected profiler.
         *
         * @param text the display text for the link
         * @param url  the URL the link points to
         * @return this builder
         */
        public ProfilerBuilder customLink(String text, String url) {
            customLinks.put(text, url);
            return this;
        }

        ExpectedProfiler build() {
            Map<String, String> links = customLinks.isEmpty() ? null : new LinkedHashMap<>(customLinks);
            return new ExpectedProfiler(rootBuilder.name, rootBuilder.build(), links);
        }
    }
}
