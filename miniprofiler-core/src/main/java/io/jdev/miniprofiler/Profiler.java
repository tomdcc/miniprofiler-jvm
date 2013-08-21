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

public interface Profiler extends Serializable, Jsonable, Closeable {
	public Timing step(String name);
	public Timing step(String name, ProfileLevel level);
	public void addQueryTiming(String query, long duration);
	public void close();
	public void stop();
	public void stop(boolean discardResults);
	public UUID getId();
	public boolean hasTrivialTimings();
	public boolean hasAllTrivialTimings();

}