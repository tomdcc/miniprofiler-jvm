/*
 * Copyright 2017 the original author or authors.
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

package io.jdev.miniprofiler.jooq;

import io.jdev.miniprofiler.ProfilerProvider;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;

/**
 * A jOOQ <code>ExecuteListenerProvider</code> that profiles SQL statements issued.
 *
 * <p>
 *     Register with your jOOQ configuration using {@link org.jooq.Configuration#set(ExecuteListenerProvider...)}.
 * </p>
 * <p>
 *     Using this class generally means that you won't need to use a JDBC driver wrapper such as
 *     {@link io.jdev.miniprofiler.sql.ProfilingDataSource} to capture SQL statements. jOOQ normally does
 *     a good job at both formatting SQL and rendering queries with inlined paramters, so if using jOOQ
 *     this way is definitely recommended.
 * </p>
 */
public class MiniProfilerExecuteListenerProvider implements ExecuteListenerProvider {

    protected final ProfilerProvider provider;

    /**
     * Construct a listener that gets the current profiler from the provided {@link ProfilerProvider}.
     * @param provider the provider to use
     */
    public MiniProfilerExecuteListenerProvider(ProfilerProvider provider) {
        this.provider = provider;
    }

    @Override
    public ExecuteListener provide() {
        return new MiniProfilerExecuteListener(provider);
    }

}
