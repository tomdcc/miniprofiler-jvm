/*
 * Copyright 2017-2026 the original author or authors.
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

import java.io.Closeable;

/**
 * A class representing a custom timing on a step, like a SQL query.
 */
public interface CustomTiming extends Closeable {

    /**
     * Returns the type of custom timing (e.g. "sql", "memcache").
     *
     * @return the execute type string
     */
    String getExecuteType();

    /**
     * Returns the command string being timed (e.g. the SQL query).
     *
     * @return the command string
     */
    String getCommandString();

    /**
     * Returns the start time in milliseconds since the profiler session started.
     *
     * @return the start time in milliseconds
     */
    long getStartMilliseconds();

    /**
     * Returns the duration in milliseconds, or null if the timing has not yet stopped.
     *
     * @return the duration in milliseconds, or null
     */
    Long getDurationMilliseconds();

    /** Stops this timing and records its duration. */
    void stop();

    /**
     * Same as calling {@link #stop()}.
     * <p>
     *     Here to satisfy {@link Closeable} and mark the method as not throwing a checked exception.
     * </p>
     */
    void close();

}
