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

package io.jdev.miniprofiler;

import io.jdev.miniprofiler.json.Jsonable;

import java.io.Closeable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Represents a step to be timed / profiled.
 *
 * <p>These are normally created using {@link Profiler#step(String)}.
 * Timing instances should always have {@link #stop()} called on
 * them. If at all possible, this should be in a finally block just
 * after the creation of the timing like so:
 * </p>
 * <blockquote><pre>
 * Timing timing = profilerProvider.getCurrentProfiler().step("some thing");
 * try {
 *     // do stuff here
 * } finally {
 *     timing.stop();
 * }
 * </pre></blockquote>
 *
 * <p>or, since the timings implements {@link Closeable}, you can use
 * Java 7 ARM blocks:</p>
 *
 * <blockquote><pre>
 * try(Timing timing = profilerProvider.getCurrentProfiler().step("some thing")) {
 *     // do stuff here
 * }
 * // automatically closed!
 * </pre></blockquote>
 */
public interface Timing extends Serializable, Jsonable, Closeable {

    /**
     * Stops the timing step. This sets the end time for the timing
     * to the current time.
     */
    void stop();

    /**
     * Returns the name of the timing
     *
     * @return the name
     */
    String getName();

    /**
     * Allows changing the name of the timing step after creation. Sometimes
     * useful when you don't know the name a particular step until later on.
     *
     * @param name new name of the timing step
     */
    void setName(String name);


    /**
     * Returns the parent timing of this one.
     *
     * @return the timing's parent, nor null for the root timing for a profiler
     */
    Timing getParent();

    /**
     * Same as calling {@link #stop()}. ere to satisfy {@link Closeable}.
     */
    void close();

    /**
     * Returned the length of this timing event
     *
     * @return the timing's duration, or null if still ongoing or the {@link #stop()} method
     *         was never called
     */
    Long getDurationMilliseconds();

    /**
     * How many hops from the root timing
     *
     * @return depth from the root timing, 0 for the root timing
     */
    int getDepth();

    /**
     * Returns custom timings
     *
     * @return all custom timings registered against this timing
     */
    Map<String, List<CustomTiming>> getCustomTimings();

    /**
     * Returns all child timings of this timing
     *
     * @return all children
     */
    List<Timing> getChildren();

    /**
     * Add a custom timing to this timing
     *
     * @param type type of timing, e.g. "sql"
     * @param executeType what type of execution, e.g. "query"
     * @param command e.g. "select * from foo"
     * @param duration how long the command took
     */
    void addCustomTiming(String type, String executeType, String command, long duration);

    /**
     * Add a custom timing to this timing
     *
     * @param type type of timing, e.g. "sql"
     * @param ct the custom timing
     */
    void addCustomTiming(String type, CustomTiming ct);

}
