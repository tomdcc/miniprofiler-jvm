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

public class MiniProfiler {
	private static ProfilerProvider profilerProvider;

	public static ProfilerProvider getProfilerProvider() {
		return profilerProvider;
	}

	public static void setProfilerProvider(ProfilerProvider profilerProvider) {
		MiniProfiler.profilerProvider = profilerProvider;
	}

	public static Profiler getCurrentProfiler() {
		return profilerProvider != null ? profilerProvider.getCurrentProfiler() : NullProfiler.INSTANCE;
	}

	public static Profiler start(String rootName) {
		return start(rootName, ProfileLevel.Info);
	}

	private static ProfilerProvider getOrCreateProfilerProvider() {
		if(profilerProvider == null) {
			// default to something hopefully sane
			profilerProvider = new DefaultProfilerProvider();
		}
		return profilerProvider;
	}

	public static Profiler start(String rootName, ProfileLevel level) {
		return getOrCreateProfilerProvider().start(rootName, level);
	}

	public static Storage getStorage() {
		return getOrCreateProfilerProvider().getStorage();
	}
}
