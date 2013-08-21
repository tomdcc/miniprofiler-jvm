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

package io.jdev.miniprofiler.sql;

import io.jdev.miniprofiler.MiniProfiler;
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.QueryTiming;
import net.sf.log4jdbc.Spy;
import net.sf.log4jdbc.SpyLogDelegator;

public class ProfilingSpyLogDelegator implements SpyLogDelegator {

    private ProfilerProvider profilerProvider;

    public ProfilingSpyLogDelegator(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
    }

    public boolean isJdbcLoggingEnabled() {
        return true;
    }

    public void exceptionOccured(Spy spy, String s, Exception e, String s2, long l) {
    }

    public void methodReturned(Spy spy, String s, String s2) {
    }

    public void constructorReturned(Spy spy, String s) {
    }

    public void sqlOccured(Spy spy, String s, String s2) {
    }

    public void sqlTimingOccured(Spy spy, long time, String method, String sql) {
        Profiler profiler = profilerProvider != null ? profilerProvider.getCurrentProfiler() : MiniProfiler.getCurrentProfiler();
		profiler.addQueryTiming(sql, time);
    }

    public void connectionOpened(Spy spy) {
    }

    public void connectionClosed(Spy spy) {
    }

    public void debug(String s) {
    }

}
