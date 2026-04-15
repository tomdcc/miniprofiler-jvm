/*
 * Copyright 2013-2026 the original author or authors.
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
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.Timing;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Concrete implementation of {@link Timing} interface.
 */
public class TimingImpl implements TimingInternal, Serializable, Jsonable {
    private static final long serialVersionUID = 1;

    private final UUID id;
    private String name;
    private final long startMilliseconds;
    private Long durationMilliseconds;
    private final ProfilerImpl profiler;
    private final TimingInternal parent;
    private final int depth;
    private List<TimingImpl> children;
    private Map<String, List<CustomTiming>> customTimings;
    private List<Profiler> childProfilers;


    private TimingImpl(ProfilerImpl profiler, TimingInternal parent, UUID id, String name,
                       long startMilliseconds, Long durationMilliseconds, int depth) {
        this.id = id;
        this.profiler = profiler;
        this.parent = parent;
        this.name = name;
        this.startMilliseconds = startMilliseconds;
        this.durationMilliseconds = durationMilliseconds;
        this.depth = depth;
    }

    TimingImpl(ProfilerImpl profiler, TimingInternal parent, String name) {
        this(profiler, parent, UUID.randomUUID(), name,
            System.currentTimeMillis() - profiler.getStarted(), null,
            parent != null ? parent.getDepth() + 1 : 0);
        if (parent != null) {
            parent.addChild(this);
        }
        profiler.setHead(this);
    }

    // Deserialization constructor — does NOT call profiler.setHead() or parent.addChild()
    TimingImpl(ProfilerImpl profiler, TimingInternal parent, UUID id, String name,
               long startMilliseconds, Long durationMilliseconds) {
        this(profiler, parent, id, name, startMilliseconds, durationMilliseconds,
            parent != null ? parent.getDepth() + 1 : 0);
    }

    @SuppressWarnings("unchecked")
    static TimingImpl fromJson(ProfilerImpl profiler, TimingInternal parent, JSONObject obj) {
        UUID id = UUID.fromString((String) obj.get("Id"));
        String name = (String) obj.get("Name");
        long startMilliseconds = ((Number) obj.get("StartMilliseconds")).longValue();
        Long durationMilliseconds = obj.get("DurationMilliseconds") != null
            ? ((Number) obj.get("DurationMilliseconds")).longValue() : null;

        TimingImpl timing = new TimingImpl(profiler, parent, id, name,
            startMilliseconds, durationMilliseconds);

        // Deserialize children
        JSONArray childrenJson = (JSONArray) obj.get("Children");
        if (childrenJson != null) {
            List<TimingImpl> children = new ArrayList<>();
            for (Object child : childrenJson) {
                children.add(TimingImpl.fromJson(profiler, timing, (JSONObject) child));
            }
            timing.children = children;
        }

        // Deserialize custom timings
        JSONObject customTimingsJson = (JSONObject) obj.get("CustomTimings");
        if (customTimingsJson != null) {
            Map<String, List<CustomTiming>> customTimings = new LinkedHashMap<>();
            for (Object entry : customTimingsJson.entrySet()) {
                Map.Entry<String, JSONArray> e = (Map.Entry<String, JSONArray>) entry;
                List<CustomTiming> list = new ArrayList<>();
                for (Object ct : e.getValue()) {
                    list.add(CustomTimingImpl.fromJson(timing, (JSONObject) ct));
                }
                customTimings.put(e.getKey(), list);
            }
            timing.customTimings = customTimings;
        }

        return timing;
    }

    @Override
    public void stop() {
        if (durationMilliseconds == null) {
            durationMilliseconds = System.currentTimeMillis() - startMilliseconds - profiler.getStarted();
        }

        profiler.setHead(parent);
    }

    @Override
    public void addChild(TimingInternal child) {
        if (children == null) {
            children = new ArrayList<>();
        }

        children.add((TimingImpl) child);
    }

    @Override
    public void addCustomTiming(String type, String executeType, String command, long duration) {
        addCustomTiming(type, CustomTimingImpl.forDuration(this, type, executeType, command, duration));
    }

    @Override
    public CustomTiming customTiming(String type, String executeType, String command) {
        return addCustomTiming(type, CustomTimingImpl.fromNow(this, type, executeType, command));
    }

    @Override
    public void customTiming(String type, String executeType, String command, Runnable block) {
        CustomTiming timing = customTiming(type, executeType, command);
        try {
            block.run();
        } finally {
            timing.stop();
        }
    }

    @Override
    public <T> T customTiming(String type, String executeType, String command, Callable<T> function) throws Exception {
        CustomTiming timing = customTiming(type, executeType, command);
        try {
            return function.call();
        } finally {
            timing.stop();
        }
    }

    private CustomTiming addCustomTiming(String type, CustomTimingImpl ct) {
        if (customTimings == null) {
            customTimings = new LinkedHashMap<String, List<CustomTiming>>();
        }
        List<CustomTiming> timingsForType = customTimings.get(type);
        if (timingsForType == null) {
            timingsForType = new ArrayList<CustomTiming>();
            customTimings.put(type, timingsForType);
        }
        timingsForType.add(ct);
        return ct;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Id", id.toString());
        map.put("Name", name);
        map.put("StartMilliseconds", startMilliseconds);
        map.put("DurationMilliseconds", durationMilliseconds);
        map.put("Children", getAllChildren());
        if (customTimings != null) {
            map.put("CustomTimings", customTimings);
        }
        return map;
    }

    @Override
    public List<Timing> getAllChildren() {
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Long getDurationMilliseconds() {
        return durationMilliseconds;
    }

    @Override
    public long getStartMilliseconds() {
        return startMilliseconds;
    }

    @Override
    public List<Timing> getChildren() {
        return children != null ? new ArrayList<Timing>(children) : Collections.<Timing>emptyList();
    }

    @Override
    public Map<String, List<CustomTiming>> getCustomTimings() {
        return customTimings;
    }

    @Override
    public ProfilerImpl getProfiler() {
        return profiler;
    }

    @Override
    public Timing getParent() {
        return parent;
    }

    @Override
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
