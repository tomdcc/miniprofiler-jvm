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

import io.jdev.miniprofiler.storage.Storage;
import io.jdev.miniprofiler.util.ResourceHelper;

import java.io.IOException;

/**
 * Convenience class for static access to profiling functions.
 *
 * <p>If your code does not use dependency injection, use this class instead
 * of a {@link ProfilerProvider} instance when you need to start a new
 * profiling session or add a new step.</p>
 *
 * <p>By default, this class will internally create a {@link DefaultProfilerProvider}
 * instance to forward calls to. If you wish to change the profiler provider used,
 * call {@link #setProfilerProvider(ProfilerProvider)} with your preferred implementation.</p>
 *
 * <p>Even if you are using dependency injection, it's probably good practice to
 * set it statically here using {@link #setProfilerProvider(ProfilerProvider)}
 * so that any code that can't get injected can still make timing calls. An example
 * might be instrumented code.</p>
 */
public class MiniProfiler {
	private static ProfilerProvider profilerProvider;
	private static String version;

	/**
	 * Return the current internal profiler provider.
	 * @return the current internal profiler provider
	 */
	public static ProfilerProvider getProfilerProvider() {
		return profilerProvider;
	}

	/**
	 * Set the profiler provider instance to use for static calls
	 * @param profilerProvider the instance to use
	 */
	public static void setProfilerProvider(ProfilerProvider profilerProvider) {
		MiniProfiler.profilerProvider = profilerProvider;
	}

	/**
	 * Returns the profiler object for the current profiling session.
	 * @return the current profiler, or a NullProfiler instance if there isn't one
	 */
	public static Profiler getCurrentProfiler() {
		return profilerProvider != null ? profilerProvider.getCurrentProfiler() : NullProfiler.INSTANCE;
	}

	static ProfilerProvider getOrCreateProfilerProvider() {
		if (profilerProvider == null) {
			// default to something hopefully sane
			profilerProvider = new DefaultProfilerProvider();
		}
		return profilerProvider;
	}

	/**
	 * Start a new profiling session with the default {@link ProfileLevel#Info} level.
	 * @param rootName the name of the root timing step to create. This would often be the URI of the current request.
	 * @return the profiler object
	 */
	public static Profiler start(String rootName) {
		return start(rootName, ProfileLevel.Info);
	}

	/**
	 * Start a new profiling session with the given level.
	 * @param rootName the name of the root timing step to create. This would often be the URI of the current request.
	 * @param level the level of logging to do
	 * @return the profiler object
	 */
	public static Profiler start(String rootName, ProfileLevel level) {
		return getOrCreateProfilerProvider().start(rootName, level);
	}

	/**
	 * Returns the storage associated with the current profiler provider. Useful
	 * for implementing a servlet etc to serve up stored profiler info.
	 * @return the current storage/
	 */
	public static Storage getStorage() {
		return getOrCreateProfilerProvider().getStorage();
	}

	/**
	 * Returns the current version of the MiniProfiler-JVM library
	 * @return the current version of the MiniProfiler-JVM library
	 */
	public static String getVersion() {
		if(version == null) {
			try {
				version = new ResourceHelper("io/jdev/miniprofiler/").getResourceAsString("miniprofiler-version.txt");
			} catch(IOException e) {
				// something prety weird going on
				version = "UNKNOWN";
			}
		}
		return version;
	}
}
