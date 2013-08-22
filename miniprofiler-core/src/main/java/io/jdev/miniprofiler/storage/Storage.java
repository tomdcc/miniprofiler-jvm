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

/**
 * Storage interface for storing profiling session information.
 *
 * <p>Profiling info is stored for later retrieval either by AJAX from the
 * mini-profiler UI on the same page, or later inspection.</p>
 */
public interface Storage {

	/**
	 * Which order to list results in.
	 */
	public static enum ListResultsOrder { Ascending, Descending }

	/**
	 * List profiling
	 * @param maxResults the maximum number of ids to list.
	 * @param start the date to start searching
	 * @param finish the end date
	 * @param orderBy which order to list the uuids
	 * @return a list of UUIDs that match the dates
	 */
	Collection<UUID> list(int maxResults, Date start, Date finish, ListResultsOrder orderBy);

	/**
	 * Stores the given profiling information
	 * @param profiler the profiling information to store
	 */
	void save(ProfilerImpl profiler);

	/**
	 * Returns a previously saved profiling session.
 	 * @param id the id of the profiling session to fetch
	 * @return the profiler, or null if not found
	 */
	ProfilerImpl load(UUID id);

	/**
	 * Sets a particular profiler session so it is considered "un-viewed" by the given user
	 * @param user the user
	 * @param id the id of the session
	 */
	void setUnviewed(String user, UUID id);

	/**
	 * Sets a particular profiler session to "viewed" for the given user.
	 * @param user the user
	 * @param id the id of the session
	 */
	void setViewed(String user, UUID id);

	/**
	 * Returns a list of profiling session ids s that haven't been seen by the given user.
	 * @param user the user
	 * @return a list of ids that the user hasn't viewed
	 */
	Collection<UUID> getUnviewedIds(String user);
}
