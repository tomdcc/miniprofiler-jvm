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

import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Profiler implementation.
 *
 * <p>Generally users of the library will not need to
 * interact directly with this class, instead getting references to the current
 * profiler through {@link io.jdev.miniprofiler.MiniProfiler#start(String)},
 * {@link io.jdev.miniprofiler.MiniProfiler#getCurrentProfiler()},
 * {@link io.jdev.miniprofiler.ProfilerProvider#start(String)} and
 * {@link io.jdev.miniprofiler.MiniProfiler#getCurrentProfiler()}, and then
 * just treating it as a {@link Profiler}.</p>
 *
 * <p>However, writers of custom {@link ProfilerProvider} implementations may
 * need to construct a new profiler by calling
 * {@link #ProfilerImpl(String, ProfileLevel, ProfilerProvider)}.</p>
 */
public class ProfilerImpl implements Profiler {
    private static final long serialVersionUID = 1;

    private final UUID id;
    private final String name;
    private final long started;
    private String user;
    private String machineName;
    private final ProfileLevel level;
    private final TimingImpl root;
    private boolean hasQueryTimings;
    private TimingImpl head;
    private boolean stopped;
    private final ProfilerProvider profilerProvider;

    /**
     * Construct a new profiling session.
     *
     * <p>This will create an implicit root {@link Timing} step considered to have
     * started now, with the given root name.</p>
     *
     * <p>A new random UUID id is created for every profiler.</p>
     *
     * <p>Any profiling steps more verbose than the given level will be ignored.</p>
     *
     * <p>The profiler provider constructing the profiler is passed in so that
     * when {@link #stop()} is called, the profiler can notify the provider to store
     * the profiling info for later retrieval via
     * {@link ProfilerProvider#stopSession(ProfilerImpl, boolean)}.</p>
     *
     * @param name             name of the request, will also be the name of the root child element
     * @param level            the level of the profiler
     * @param profilerProvider the profiler provider constructing the
     */
    public ProfilerImpl(String name, ProfileLevel level, ProfilerProvider profilerProvider) {
        this(name, name, level, profilerProvider);
    }

    /**
     * Construct a new profiling session.
     *
     * <p>This will create an implicit root {@link Timing} step considered to have
     * started now, with the given root name.</p>
     *
     * <p>A new random UUID id is created for every profiler.</p>
     *
     * <p>Any profiling steps more verbose than the given level will be ignored.</p>
     *
     * <p>The profiler provider constructing the profiler is passed in so that
     * when {@link #stop()} is called, the profiler can notify the provider to store
     * the profiling info for later retrieval via
     * {@link ProfilerProvider#stopSession(ProfilerImpl, boolean)}.</p>
     *
     * @param name             name of the request
     * @param rootName         name of the root timing step to start
     * @param level            the level of the profiler
     * @param profilerProvider the profiler provider constructing the
     */
    public ProfilerImpl(String name, String rootName, ProfileLevel level, ProfilerProvider profilerProvider) {
        this(null, name, rootName, level, profilerProvider);
    }

    /**
     * Construct a new profiling session.
     *
     * <p>This will create an implicit root {@link Timing} step considered to have
     * started now, with the given root name.</p>
     *
     * <p>A new random UUID id is created for every profiler.</p>
     *
     * <p>Any profiling steps more verbose than the given level will be ignored.</p>
     *
     * <p>The profiler provider constructing the profiler is passed in so that
     * when {@link #stop()} is called, the profiler can notify the provider to store
     * the profiling info for later retrieval via
     * {@link ProfilerProvider#stopSession(ProfilerImpl, boolean)}.</p>
     *
     * @param id               the id to use, or null to generate a random uuid
     * @param name             name of the request
     * @param rootName         name of the root timing step to start
     * @param level            the level of the profiler
     * @param profilerProvider the profiler provider constructing the
     */
    public ProfilerImpl(UUID id, String name, String rootName, ProfileLevel level, ProfilerProvider profilerProvider) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.profilerProvider = profilerProvider;
        this.level = level;
        started = System.currentTimeMillis();
        root = new TimingImpl(this, null, rootName);
        head = root;
    }

    public long getDurationMilliseconds() {
        Long milliseconds = root.getDurationMilliseconds();
        return milliseconds != null ? milliseconds : System.currentTimeMillis() - started;
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean discardResults) {
        if (!stopped) {
            stopped = true;
            root.stop();
            profilerProvider.stopSession(this, discardResults);
        }
    }

    public Timing step(String name, ProfileLevel level) {
        if (level.ordinal() > this.level.ordinal()) {
            return NullTiming.INSTANCE;
        } else {
            return new TimingImpl(this, head, name);
        }
    }

    public Timing step(String name) {
        return step(name, ProfileLevel.Info);
    }

    public void addCustomTiming(String type, String executeType, String command, long duration) {
        addCustomTiming(type, new CustomTiming(executeType, command, duration));
    }

    public void addCustomTiming(String type, CustomTiming customTiming) {
        if (head != null) {
            head.addCustomTiming(type, customTiming);
        }
    }


    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(11);
        map.put("Id", id.toString());
        map.put("Name", name);
        map.put("Started", started);
        map.put("DurationMilliseconds", getDurationMilliseconds());
        map.put("MachineName", machineName);
        map.put("Root", root.toMap());
        // TODO support ClientTimings and CustomLinks
        map.put("ClientTimings", null);
        return map;
    }

    public UUID getId() {
        return id;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public ProfileLevel getLevel() {
        return level;
    }

    public Timing getRoot() {
        return root;
    }

    public boolean hasQueryTimings() {
        return hasQueryTimings;
    }

    public Timing getHead() {
        return head;
    }

    public void setHead(TimingImpl head) {
        this.head = head;
    }

    public long getStarted() {
        return started;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    public void setHasQueryTimings(boolean hasQueryTimings) {
        this.hasQueryTimings = hasQueryTimings;
    }

    @Override
    public void close() {
        stop();
    }
}
