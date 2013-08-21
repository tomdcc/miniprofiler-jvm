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

import java.util.*;

public class ProfilerImpl implements Profiler {
    private static final long serialVersionUID = 1;

    private final UUID id;
    private final long started;
	private String user;
    private String machineName;
    private final ProfileLevel level;
    private final TimingImpl root;
    private boolean hasQueryTimings = false;
    private TimingImpl head;
	private List<TimingImpl> flattenedTimings;
	private boolean stopped = false;
	private final ProfilerProvider profilerProvider;

    public ProfilerImpl(String rootName, ProfileLevel level, ProfilerProvider profilerProvider) {
        id = UUID.randomUUID();
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

    public boolean hasTrivialTimings() {
		for(TimingImpl t : getTimingHierarchy()) {
			if(t.isTrivial()) {
				return true;
			}
		}
        return false;
    }

    public boolean hasAllTrivialTimings() {
		for(TimingImpl t : getTimingHierarchy()) {
			if(!t.isTrivial()) {
				return false;
			}
		}
        return true;
    }

    public int getTrivialDurationThresholdMilliseconds() {
        // TODO: implement with some sort of config
        return 2;
    }

	public void stop() {
		stop(false);
	}

    public void stop(boolean discardResults) {
		if(!stopped) {
			stopped = true;
			root.stop();
			profilerProvider.stopSession(this, discardResults);
		}
    }

    public Timing step(String name, ProfileLevel level) {
        if (level.ordinal() > this.level.ordinal()) return NullTiming.INSTANCE;
        return new TimingImpl(this, head, name);
    }

    public Timing step(String name) {
        return step(name, ProfileLevel.Info);
    }

	public void addQueryTiming(String query, long duration) {
		addQueryTiming(new QueryTiming(query, duration));
	}

    public void addQueryTiming(QueryTiming sqlTiming) {
        if (head == null) return;

        // TODO: implement SQL duplicates
        head.addQueryTiming(sqlTiming);
    }

    public long getDurationMillisecondsInSql() {
        long total = 0;
        for(TimingImpl t : getTimingHierarchy()) {
            if(t.hasQueryTimings()) {
                for(QueryTiming sql : t.getQueryTimings()) {
                    total += sql.getDurationMilliseconds();
                }
            }
        }
        return total;
    }

    public List<TimingImpl> getTimingHierarchy() {
		if(flattenedTimings != null) {
			return flattenedTimings;
		}
        ArrayList<TimingImpl> result = new ArrayList<TimingImpl>();
        Stack<TimingImpl> stack = new Stack<TimingImpl>();
        stack.push(root);

        while (stack.size() > 0) {
            TimingImpl timing = stack.pop();
            result.add(timing);
            if (timing.hasChildren()) {
                stack.addAll(timing.getChildren());
            }
        }

		if(stopped) {
			flattenedTimings = result;
		}
        return result;
    }

    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(11);
        map.put("Id", id.toString());
        map.put("DurationMilliseconds", getDurationMilliseconds());
        map.put("HasTrivialTimings", hasTrivialTimings());
        map.put("HasAllTrivialTimings", hasAllTrivialTimings());
        map.put("HasSqlTimings", hasQueryTimings);
        map.put("HasDuplicateSqlTimings", false);
        map.put("MachineName", machineName);
        map.put("User", user);
        map.put("Started", "/Date(" + String.valueOf(started) + ")");
        map.put("CustomTimingNames", new ArrayList<Object>());
        map.put("Root", root.toMap());
        map.put("ClientTimings", null);
        map.put("DurationMillisecondsInSql", getDurationMillisecondsInSql());
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

	public String getUser() {
		return user;
	}

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
