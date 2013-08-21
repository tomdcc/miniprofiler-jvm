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

import io.jdev.miniprofiler.json.JsonUtil;

import java.util.*;

public class TimingImpl implements Timing {
	private static final long serialVersionUID = 1;

	private final UUID id;
	private final String name;
	private final long startMilliseconds;
	private Long durationMilliseconds;
	private final ProfilerImpl profiler;
	private final TimingImpl parent;
	private final int depth;
	private List<TimingImpl> children;
	private Map<String, String> keyValues;
	private List<QueryTiming> queryTimings;


	public TimingImpl(ProfilerImpl profiler, TimingImpl parent, String name) {
		this.id = UUID.randomUUID();
		this.profiler = profiler;
		this.name = name;
		this.parent = parent;
		startMilliseconds = System.currentTimeMillis() - profiler.getStarted();

		// root will have no parent
		if (parent != null) {
			parent.addChild(this);
			depth = parent.depth + 1;
		} else {
			depth = 0;
		}

		profiler.setHead(this);
	}

	public void stop() {
		if (durationMilliseconds == null) {
			durationMilliseconds = System.currentTimeMillis() - startMilliseconds - profiler.getStarted();
		}

		profiler.setHead(parent);
	}

	public void addChild(TimingImpl child) {
		if (children == null) {
			children = new ArrayList<TimingImpl>();
		}

		children.add(child);
	}

	public void addQueryTiming(String query, long duration) {
		addQueryTiming(new QueryTiming(query, duration));
	}

	public void addQueryTiming(QueryTiming qt) {
		if (queryTimings == null) {
			queryTimings = new ArrayList<QueryTiming>();
		}
		queryTimings.add(qt);
		qt.setParentTiming(this);
		profiler.setHasQueryTimings(true);
	}

	public long getDurationWithoutChildrenMilliseconds() {
		if (durationMilliseconds == null) return 0L;

		long duration = durationMilliseconds;
		if (children != null) {
			for (TimingImpl child : children) {
				final Long milliseconds = child.durationMilliseconds;
				duration -= (milliseconds != null) ? milliseconds : 0L;
			}
		}
		return duration;
	}

	public long getQueryTimingsDurationMilliseconds() {
		long duration = 0;
		if (hasQueryTimings()) {
			for (QueryTiming timing : queryTimings) {
				duration += timing.getDurationMilliseconds();
			}

		}

		return duration;
	}

	public boolean hasQueryTimings() {
		return queryTimings != null && !queryTimings.isEmpty();
	}

	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}

	public boolean isTrivial() {
		return durationMilliseconds < profiler.getTrivialDurationThresholdMilliseconds();
	}

	public Map<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(13);
		map.put("Id", id.toString());
		map.put("Name", name);
		map.put("DurationMilliseconds", durationMilliseconds);
		map.put("DurationWithoutChildrenMilliseconds", getDurationWithoutChildrenMilliseconds());
		map.put("StartMilliseconds", startMilliseconds);
		map.put("KeyValues", keyValues);
		map.put("HasSqlTimings", hasQueryTimings());
		map.put("SqlTimingsDurationMilliseconds", getQueryTimingsDurationMilliseconds());
		map.put("SqlTimings", JsonUtil.mapList(queryTimings));
		map.put("Depth", depth);
		map.put("IsTrivial", isTrivial());
		map.put("HasChildren", hasChildren());
		map.put("Children", JsonUtil.mapList(children));
		return map;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Long getDurationMilliseconds() {
		return durationMilliseconds;
	}

	public void setDurationMilliseconds(long durationMilliseconds) {
		this.durationMilliseconds = durationMilliseconds;
	}

	public long getStartMilliseconds() {
		return startMilliseconds;
	}

	public List<TimingImpl> getChildren() {
		return children;
	}

	public Map<String, String> getKeyValues() {
		return keyValues;
	}

	public void setKeyValues(Map<String, String> keyValues) {
		this.keyValues = keyValues;
	}

	public List<QueryTiming> getQueryTimings() {
		return queryTimings;
	}

	public ProfilerImpl getProfiler() {
		return profiler;
	}

	public TimingImpl getParent() {
		return parent;
	}

	public int getDepth() {
		return depth;
	}

	@Override
	public void close() {
		stop();
	}
}
