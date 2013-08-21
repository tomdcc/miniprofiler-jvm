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
import io.jdev.miniprofiler.sql.SqlFormatterFactory;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class QueryTiming implements Serializable, Jsonable {
    private UUID id;
    private String commandString;
    private TimingImpl parentTiming;
    private long absoluteStartMilliseconds;
    private long durationMilliseconds;
    private long miniprofilerStartMilliseconds = -1L;

    public QueryTiming(String commandString) {
        id = UUID.randomUUID();
        this.commandString = commandString;
        // TODO: stack traces?
		absoluteStartMilliseconds = System.currentTimeMillis();
    }

	public QueryTiming(String sql, long duration) {
		this(sql);
		setDurationMilliseconds(duration);
	}

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(8);
        map.put("Id", id.toString());
        map.put("FormattedCommandString", SqlFormatterFactory.getFormatter().format(commandString));
        map.put("ParentTimingName", parentTiming.getName());
        map.put("ParentTimingId", parentTiming.getId().toString());
        map.put("StartMilliseconds", getStartMilliseconds());
        map.put("ExecuteType", 0);
        map.put("DurationMilliseconds", durationMilliseconds);
        map.put("StackTraceSnippet", "");
        return map;
    }

    public UUID getId() {
        return id;
    }

    public String getCommandString() {
        return commandString;
    }

    public TimingImpl getParentTiming() {
        return parentTiming;
    }

    public void setParentTiming(TimingImpl parentTiming) {
        this.parentTiming = parentTiming;
		miniprofilerStartMilliseconds = parentTiming.getProfiler().getStarted();
    }

    public long getStartMilliseconds() {
		if(miniprofilerStartMilliseconds < 0L) {
			throw new IllegalStateException("Can't determine start until mini profiler start is set");
		}
        return absoluteStartMilliseconds - miniprofilerStartMilliseconds;
    }

    public long getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public void setDurationMilliseconds(long durationMilliseconds) {
        this.durationMilliseconds = durationMilliseconds;
    }

	public void setMiniprofilerStartMilliseconds(long miniprofilerStartMilliseconds) {
		this.miniprofilerStartMilliseconds = miniprofilerStartMilliseconds;
	}
}
