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

/**
 * A profiler provider which keeps track of the current profiling
 * session in an internal {@link ThreadLocal} variable. This should work
 * pretty well for most thread-as-worker based systems.
 */
public class DefaultProfilerProvider extends BaseProfilerProvider {

	private static final ThreadLocal<Profiler> profilerHolder = new ThreadLocal<Profiler>();

	@Override
	protected void profilerCreated(Profiler profiler) {
		profilerHolder.set(profiler);
	}

	@Override
	protected void profilerStopped(Profiler profiler) {
		profilerHolder.remove();
	}

	/**
	 * Returns the current MiniProfiler.
	 */
	@Override
	public Profiler lookupCurrentProfiler() {
		return profilerHolder.get();
	}

}
