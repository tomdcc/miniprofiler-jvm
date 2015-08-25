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

public class CustomTiming implements Serializable, Jsonable {
    private UUID id;
    private String executeType;
    private String commandString;
    private TimingImpl parentTiming;
    private long absoluteStartMilliseconds;
    private long durationMilliseconds;
    private long miniprofilerStartMilliseconds = -1L;

    public CustomTiming(String executeType, String commandString) {
        id = UUID.randomUUID();
        this.executeType = executeType;
        this.commandString = commandString;
        // TODO: stack traces
        absoluteStartMilliseconds = System.currentTimeMillis();
    }

    public CustomTiming(String executeType, String command, long duration) {
        this(executeType, command);
        setDurationMilliseconds(duration);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(8);
        map.put("Id", id.toString());
        if (executeType != null) {
            map.put("ExecuteType", executeType);
        }
        map.put("CommandString", SqlFormatterFactory.getFormatter().format(commandString));
        map.put("StartMilliseconds", getStartMilliseconds());
        map.put("DurationMilliseconds", durationMilliseconds);
        map.put("StackTraceSnippet", "");
        // TODO FirstFetchDurationMilliseconds
        return map;
    }

    public UUID getId() {
        return id;
    }

    public String getCommandString() {
        return commandString;
    }

    void setParentTiming(TimingImpl parentTiming) {
        // stored as we'll use this in sql storage
        this.parentTiming = parentTiming;
        miniprofilerStartMilliseconds = parentTiming.getProfiler().getStarted();
    }

    public TimingImpl getParentTiming() {
        return parentTiming;
    }

    private long getStartMilliseconds() {
        if (miniprofilerStartMilliseconds < 0L) {
            throw new IllegalStateException("Can't determine start until mini profiler start is set");
        }
        return absoluteStartMilliseconds - miniprofilerStartMilliseconds;
    }

    public void setDurationMilliseconds(long durationMilliseconds) {
        this.durationMilliseconds = durationMilliseconds;
    }

}
