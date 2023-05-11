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

package io.jdev.miniprofiler.internal;

import io.jdev.miniprofiler.CustomTiming;
import io.jdev.miniprofiler.sql.SqlFormatterFactory;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

class CustomTimingImpl implements CustomTiming, Serializable, Jsonable {
    private static final long serialVersionUID = 1;

    private final TimingInternal parentTiming;
    private final UUID id;
    private final String executeType;
    private final String commandString;
    private final long startMilliseconds;
    private Long durationMilliseconds;
    // TODO: stack traces
    // TODO FirstFetchDurationMilliseconds

    private CustomTimingImpl(TimingInternal parentTiming, String executeType, String commandString, long startMilliseconds, Long durationMilliseconds) {
        id = UUID.randomUUID();
        this.executeType = executeType;
        this.commandString = commandString;
        this.parentTiming = parentTiming;
        this.startMilliseconds = startMilliseconds;
        this.durationMilliseconds = durationMilliseconds;
    }

    static CustomTimingImpl forDuration(TimingImpl parentTiming, String executeType, String command, long duration) {
        return forDurationFrom(parentTiming, executeType, command, duration, System.currentTimeMillis());
    }

    static CustomTimingImpl forDurationFrom(TimingImpl parentTiming, String executeType, String command, long duration, long start) {
        return new CustomTimingImpl(parentTiming, executeType, command, relativeToProfilerStart(parentTiming, start) - duration, duration);
    }

    static CustomTimingImpl fromNow(TimingImpl parentTiming, String executeType, String command) {
        return from(parentTiming, executeType, command, System.currentTimeMillis());
    }

    static CustomTimingImpl from(TimingImpl parentTiming, String executeType, String command, long start) {
        return new CustomTimingImpl(parentTiming, executeType, command, relativeToProfilerStart(parentTiming, start), null);
    }

    private static long relativeToProfilerStart(TimingInternal parentTiming, long time) {
        return time - parentTiming.getProfiler().getStarted();
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Id", id.toString());
        if (executeType != null) {
            map.put("ExecuteType", executeType);
        }
        map.put("CommandString", SqlFormatterFactory.getFormatter().format(commandString));
        map.put("StartMilliseconds", getStartMilliseconds());
        map.put("DurationMilliseconds", durationMilliseconds);
        map.put("StackTraceSnippet", "");
        return map;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String getCommandString() {
        return commandString;
    }

    @Override
    public String getExecuteType() {
        return executeType;
    }

    @Override
    public long getStartMilliseconds() {
        return startMilliseconds;
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public void stop() {
        stop(System.currentTimeMillis());
    }

    void stop(long at) {
        if (this.durationMilliseconds == null) {
            this.durationMilliseconds = relativeToProfilerStart(parentTiming, at) - startMilliseconds;
        }
    }

    @Override
    public Long getDurationMilliseconds() {
        return durationMilliseconds;
    }
}
