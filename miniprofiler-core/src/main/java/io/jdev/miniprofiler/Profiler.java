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

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Represents a single profiling session.
 *
 * <p>Typical usage involves starting a profiling session by calling
 * {@link ProfilerProvider#start(String, ProfileLevel)} or
 * {@link MiniProfiler#start(String, ProfileLevel)}. A profiling session
 * should be stopped by calling {@link #stop()}. Typically this might be
 * done in a finally block around some code:</p>
 *
 * <blockquote><pre>
 * Profiler profiler = MiniProfiler.start("/my/uri", Info);
 * try {
 *     // do stuff here
 * } finally {
 *     profiler.stop();
 * }
 * </pre></blockquote>
 *
 * <p>or, since the interface implements {@link Closeable}, you can use
 * Java 7 ARM blocks:</p>
 *
 * <blockquote><pre>
 * try(Profiler profiler = profilerProvider.start("/my/uri", Info)) {
 *     // do stuff here
 * }
 * </pre></blockquote>
 *
 * <p>Individual profiling steps can then be added by calling
 * {@link #step(String, ProfileLevel)}. Usually it would be necessary
 * to get a handle on the current profiler first, using either
 * {@link io.jdev.miniprofiler.ProfilerProvider#current()}
 * or {@link io.jdev.miniprofiler.MiniProfiler#current()}.
 * </p>
 * <blockquote><pre>
 * try(Timing profiler = profilerProvider.current().step("Some complicated step", Info)) {
 *     // do stuff here
 * }
 * </pre></blockquote>
 *
 * <p>Calls can be nested, and nested steps will be displayed indented in
 * the UI.</p>
 * <blockquote><pre>
 * try(Timing profiler = profilerProvider.current().step("Some complicated step", Info)) {
 *     // do stuff here
 *     try(Timing profiler = profilerProvider.current().step("Another step inside that one!", Info)) {
 *         // do stuff here
 *     }
 * }
 * </pre></blockquote>
 */
public interface Profiler extends Closeable {

    /**
     * The name of the profiling session.
     *
     * @return the name of the profiling session.
     */
    String getName();

    /**
     * Start a new profiling step with the default {@link ProfileLevel#Info} level;
     *
     * @param name The name of the step.
     * @return a Timing instance to be stopped when the step is done
     */
    Timing step(String name);

    /**
     * Start a new profiling step with the given level;
     *
     * @param name  The name of the step.
     * @param level the profiling level for this step
     * @return a Timing instance to be stopped when the step is done
     */
    Timing step(String name, ProfileLevel level);

    /**
     * Start and stop a new profiling step with the given block.
     *
     * @param name  The name of the step.
     * @param block The block to time
     */
    void step(String name, Runnable block);

    /**
     * Start and stop a new profiling step with the given block.
     *
     * @param name  The name of the step.
     * @param level The profiling level for this step
     * @param block The block to time
     */
    void step(String name, ProfileLevel level, Runnable block);

    /**
     * Start and stop a new profiling step with the given callable function.
     *
     * @param name  The name of the step.
     * @param function The function to time
     * @param <T> the return type of the function
     * @return the result of calling the function
     * @throws Exception when the function throws an exception
     */
    <T> T step(String name, Callable<T> function) throws Exception;

    /**
     * Start and stop a new profiling step with the given block.
     *
     * @param name  The name of the step.
     * @param level The profiling level for this step
     * @param function The function to time
     * @param <T> the return type of the function
     * @return the result of calling the function
     * @throws Exception when the function throws an exception
     */
    <T> T step(String name, ProfileLevel level, Callable<T> function) throws Exception;

    /**
     * Add a query timing inside the current timing step. This is usually
     * to log an SQL or other query.
     *
     * @param type        the type of query, e.g. sql, memcache etc
     * @param executeType the type of command executed, e.g. read, fetch, update etc
     * @param command     the query to save
     * @param duration    how long it took, in milliseconds
     */
    void addCustomTiming(String type, String executeType, String command, long duration);

    /**
     * Starts a custom timing under this timing.
     *
     * <p>
     *     The custom timing will not have a duration until {@link CustomTiming#stop()} is called on the returned object.
     * </p>
     *
     * @param type type of timing, e.g. "sql"
     * @param executeType what type of execution, e.g. "query"
     * @param command e.g. "select * from foo"
     * @return the custom timing created
     */
    CustomTiming customTiming(String type, String executeType, String command);

    /**
     * Start and stop a new custom timing with the given block.
     * @param type type of timing, e.g. "sql"
     * @param executeType what type of execution, e.g. "query"
     * @param command e.g. "select * from foo"
     * @param block the code to run
     */
    void customTiming(String type, String executeType, String command, Runnable block);

    /**
     * Start and stop a new custom timing with the given callable function.
     *
     * @param type type of timing, e.g. "sql"
     * @param executeType what type of execution, e.g. "query"
     * @param command e.g. "select * from foo"
     * @param function The function to time
     * @param <T> the return type of the function
     * @return the result of calling the function
     * @throws Exception when the function throws an exception
     */
    <T> T customTiming(String type, String executeType, String command, Callable<T> function) throws Exception;

    /**
     * Stop the current timing session.
     */
    void stop();

    /**
     * Stop the current timing session. If passed true, the
     * current profiling session won't be saved for later viewing.
     * This is usually if the page has been cancelled for some reason.
     *
     * @param discardResults whether to skip storing the current session data
     */
    void stop(boolean discardResults);

    /**
     /**
     * Same as calling {@link #stop()}.
     *
     * <p>
     *     Here to satisfy {@link Closeable} and mark the method as not throwing a checked exception.
     * </p>
     */
    void close();

    /**
     * Returns a unique id for the profiling session.
     *
     * @return the session's id
     */
    UUID getId();


    /**
     * Returns the current live timing for this profiler.
     *
     * @return the current timing
     */
    Timing getHead();

    /**
     * Returns the root timing for this profiler.
     *
     * @return the root timing
     */
    Timing getRoot();

    /**
     * Set the user name for the current profiling session
     *
     * @param user the user
     */
    void setUser(String user);

    /**
     * Returns the user name for the profile session.
     *
     * @return the current user
     */
    String getUser();

    /**
     * Adds a child profiler to the current on.
     *
     * @param name The name to give the child profiler
     * @return the child profiler
     */
    Profiler addChild(String name);

    /**
     * Returns a JSON string representing this profiler instance in a form understood by the Javascript UI.
     *
     * @return a JSON string
     */
    String asUiJson();

    /**
     * Render a plain test version of this profiler, for logging
     *
     * @return the plain text representation
     */
    String asPlainText();
}
