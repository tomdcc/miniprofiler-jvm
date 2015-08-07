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
import java.util.UUID;

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
 * {@link io.jdev.miniprofiler.ProfilerProvider#getCurrentProfiler()}
 * or {@link io.jdev.miniprofiler.MiniProfiler#getCurrentProfiler()}.
 * </p>
 * <blockquote><pre>
 * try(Timing profiler = profilerProvider.getCurrentProfiler().step("Some complicated step", Info)) {
 *     // do stuff here
 * }
 * </pre></blockquote>
 *
 * <p>Calls can be nested, and nested steps will be displayed indented in
 * the UI.</p>
 * <blockquote><pre>
 * try(Timing profiler = profilerProvider.getCurrentProfiler().step("Some complicated step", Info)) {
 *     // do stuff here
 *     try(Timing profiler = profilerProvider.getCurrentProfiler().step("Another step inside that one!", Info)) {
 *         // do stuff here
 *     }
 * }
 * </pre></blockquote>
 */
public interface Profiler extends Serializable, Jsonable, Closeable {

	/**
	 * Start a new profiling step with the default {@link ProfileLevel#Info} level;
	 *
	 * @param name The name of the step.
	 * @return a Timing instance to be stopped when the step is done
	 */
	public Timing step(String name);

	/**
	 * Start a new profiling step with the given level;
	 *
	 * @param name The name of the step.
	 * @param level the profiling level for this step
	 * @return a Timing instance to be stopped when the step is done
	 */
	public Timing step(String name, ProfileLevel level);

	/**
	 * Add a query timing inside the current timing step. This is usually
	 * to log an SQL or other query.
	 *
	 * @param type the type of query, e.g. sql, memcache etc
     * @param executeType the type of command executed, e.g. read, fetch, update etc
	 * @param command the query to save
	 * @param duration how long it took, in milliseconds
	 */
	public void addCustomTiming(String type, String executeType, String command, long duration);

	/**
	 * Stop the current timing session.
	 */
	public void stop();

	/**
	 * Stop the current timing session. If passed true, the
	 * current profiling session won't be saved for later viewing.
	 * This is usually if the page has been cancelled for some reason.
	 *
	 * @param discardResults whether to skip storing the current session data
	 */
	public void stop(boolean discardResults);

	/**
	 * Same as calling {@link #stop()}. Here to satisfy {@link Closeable} so
	 * that the profiler can be auto-closed in a Java 7 try-with-resources
	 * block.
	 */
	public void close();

	/**
	 * Returns a unique id for the profiling session.
	 * @return the session's id
	 */
	public UUID getId();


	/**
	 * Returns the current live timing for this profiler.
	 * @return the current timing
	 */
	public Timing getHead();

	/**
	 * Set the user name for the current profiling session
	 * @param user the user
	 */
	public void setUser(String user);

	/**
	 * Returns the user name for the profile session.
	 * @return the current user
	 */
	public String getUser();
}
