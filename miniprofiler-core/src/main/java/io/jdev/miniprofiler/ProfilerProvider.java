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

public interface ProfilerProvider {
	Profiler start(String rootName);
    Profiler start(String rootName, ProfileLevel level);

    /**
     * Ends the current profiling session, if one exists.
     *
     * @param discardResults When true, clears the miniprofiler for this request, allowing profiling to
     *                       be prematurely stopped and discarded. Useful for when a specific route does not need to be profiled.
     */
	void stopCurrentSession(boolean discardResults);

	/**
	 * Marks the given profiling session as stopped.
	 *
	 * @param profilingSession the profiler to register as stopped
	 * @param discardResults When true, clears the miniprofiler for this request, allowing profiling to
	 *        be prematurely stopped and discarded. Useful for when a specific route does not need to be profiled.
	 */
	void stopSession(ProfilerImpl profilingSession, boolean discardResults);

    /**
     * Returns the current MiniProfiler.
     */
    Profiler getCurrentProfiler();

	Storage getStorage();
}
