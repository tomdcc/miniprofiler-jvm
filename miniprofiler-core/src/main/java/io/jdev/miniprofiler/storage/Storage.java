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

package io.jdev.miniprofiler.storage;

import io.jdev.miniprofiler.ProfilerImpl;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

public interface Storage {

	public static enum ListResultsOrder { Ascending, Descending }

	/// <summary>
	/// list the result keys.
	/// </summary>
	/// <param name="maxResults">The max results.</param>
	/// <param name="start">The start.</param>
	/// <param name="finish">The finish.</param>
	/// <param name="orderBy">order by.</param>
	/// <returns>the list of keys in the result.</returns>
	Collection<UUID> list(int maxResults, Date start, Date finish, ListResultsOrder orderBy);

	/// <summary>
	/// Stores <paramref name="profiler"/> under its <see cref="MiniProfiler.Id"/>.
	/// </summary>
	/// <param name="profiler">The results of a profiling session.</param>
	/// <remarks>
	/// Should also ensure the profiler is stored as being un-viewed by its profiling <see cref="MiniProfiler.User"/>.
	/// </remarks>
	void save(ProfilerImpl profiler);

	/// <summary>
	/// Returns a <see cref="MiniProfiler"/> from storage based on <paramref name="id"/>, which should map to <see cref="MiniProfiler.Id"/>.
	/// </summary>
	/// <param name="id">
	/// The id.
	/// </param>
	/// <remarks>
	/// Should also update that the resulting profiler has been marked as viewed by its profiling <see cref="MiniProfiler.User"/>.
	/// </remarks>
	/// <returns>
	/// The <see cref="MiniProfiler"/>.
	/// </returns>
	ProfilerImpl load(UUID id);

	/// <summary>
	/// Sets a particular profiler session so it is considered "un-viewed"
	/// </summary>
	/// <param name="user">
	/// The user.
	/// </param>
	/// <param name="id">
	/// The id.
	/// </param>
	void setUnviewed(String user, UUID id);

	/// <summary>
	/// Sets a particular profiler session to "viewed"
	/// </summary>
	/// <param name="user">
	/// The user.
	/// </param>
	/// <param name="id">
	/// The id.
	/// </param>
	void setViewed(String user, UUID id);

	/// <summary>
	/// Returns a list of <see cref="MiniProfiler.Id"/>s that haven't been seen by <paramref name="user"/>.
	/// </summary>
	/// <param name="user">
	/// User identified by the current <c>MiniProfiler.Settings.UserProvider</c>
	/// </param>
	/// <returns>the list of key values.</returns>
	Collection<UUID> getUnviewedIds(String user);
}
