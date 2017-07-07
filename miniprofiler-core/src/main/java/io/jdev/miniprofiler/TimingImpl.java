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

/**
 * Concrete implementation of {@link Timing} interface.
 */
public class TimingImpl implements Timing {
    private static final long serialVersionUID = 1;

    private final UUID id;
    private String name;
    private final long startMilliseconds;
    private Long durationMilliseconds;
    private final ProfilerImpl profiler;
    private final TimingImpl parent;
    private final int depth;
    private List<TimingImpl> children;
    private Map<String, List<CustomTiming>> customTimings;
    private List<Profiler> childProfilers;


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

    public void addCustomTiming(String type, String executeType, String command, long duration) {
        addCustomTiming(type, new CustomTiming(executeType, command, duration));
    }

    public void addCustomTiming(String type, CustomTiming qt) {
        if (customTimings == null) {
            customTimings = new LinkedHashMap<String, List<CustomTiming>>();
        }
        List<CustomTiming> timingsForType = customTimings.get(type);
        if (timingsForType == null) {
            timingsForType = new ArrayList<CustomTiming>();
            customTimings.put(type, timingsForType);
        }
        timingsForType.add(qt);
        qt.setParentTiming(this);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(13);
        map.put("Id", id.toString());
        map.put("Name", name);
        map.put("StartMilliseconds", startMilliseconds);
        map.put("DurationMilliseconds", durationMilliseconds);
        map.put("Children", JsonUtil.mapList(allChildren()));
        if (customTimings != null) {
            map.put("CustomTimings", customTimings);
        }
        return map;
    }

    private List<Timing> allChildren() {
        if (children == null && childProfilers == null) {
            return null;
        }
        List<Timing> kids = new ArrayList<Timing>(children == null ? Collections.<Timing>emptyList() : children);
        if (childProfilers != null) {
            for (Profiler childProfiler : childProfilers) {
                kids.add(childProfiler.getRoot());
            }
        }
        Collections.sort(kids, new Comparator<Timing>() {
            @Override
            public int compare(Timing t1, Timing t2) {
                return (int) (t1.getStartMilliseconds() - t2.getStartMilliseconds());
            }
        });
        return kids;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<Timing> getChildren() {
        return children != null ? new ArrayList<Timing>(children) : Collections.<Timing>emptyList();
    }

    public Map<String, List<CustomTiming>> getCustomTimings() {
        return customTimings;
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

    @Override
    public Profiler addChildProfiler(String name) {
        if (childProfilers == null) {
            childProfilers = new ArrayList<Profiler>();
        }
        ProfilerImpl child = new ProfilerImpl("\u2443 " + name, profiler.getLevel(), profiler.getStarted());
        childProfilers.add(child);
        return child;
    }

    @Override
    public List<Profiler> getChildProfilers() {
        return childProfilers;
    }
}
