/*
 * Copyright 2026 the original author or authors.
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single client-side performance timing entry.
 */
public class ClientTiming implements Jsonable {

    private final String name;
    private final long start;
    private final Long duration;

    ClientTiming(String name, long start, Long duration) {
        this.name = name;
        this.start = start;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public long getStart() {
        return start;
    }

    public Long getDuration() {
        return duration;
    }

    @SuppressWarnings("unchecked")
    public static ClientTiming fromJson(JSONObject obj) {
        String name = (String) obj.get("Name");
        long start = ((Number) obj.get("Start")).longValue();
        Long duration = obj.containsKey("Duration") && obj.get("Duration") != null
            ? ((Number) obj.get("Duration")).longValue()
            : null;
        return new ClientTiming(name, start, duration);
    }

    @SuppressWarnings("unchecked")
    public static List<ClientTiming> listFromJson(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return null;
        }
        List<ClientTiming> result = new ArrayList<>(array.size());
        for (Object item : array) {
            result.add(fromJson((JSONObject) item));
        }
        return result;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Name", name);
        map.put("Start", start);
        if (duration != null) {
            map.put("Duration", duration);
        }
        return map;
    }
}
