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
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;

import static io.jdev.miniprofiler.jooq.MiniProfilerJooqUtil.renderInlined;

/**
 * Captures sql statements from jOOQ during profiling.
 *
 * <p>
 *     This can give better results than wrapping the JDBC driver and provides statements with inlined
 *     parameters for easy copy/paste.
 * </p>
 * <p>
 *     It is recommended to use the {@link MiniProfilerExecuteListenerProvider} rather than use this class
 *     directly. It's left public to allow customisation.
 * </p>
 *
 * @see MiniProfilerExecuteListenerProvider
 * @see org.jooq.ExecuteListener
 */
@SuppressWarnings("WeakerAccess")
public class MiniProfilerExecuteListener extends DefaultExecuteListener {

    /** The profiler provider used to record SQL timings. */
    protected final ProfilerProvider provider;
    /** The start time in milliseconds of the current SQL execution. */
    private long start;

    /**
     * Constructs a listener with the given profiler.
     * @param provider the profiler provider to use
     */
    public MiniProfilerExecuteListener(ProfilerProvider provider) {
        this.provider = provider;
    }

    /** Records the start time of the SQL execution. */
    @Override
    public void start(ExecuteContext ctx) {
        start = System.currentTimeMillis();
    }

    /** Records the timing if a profiler is active and the context contains a SQL statement. */
    @Override
    public void end(ExecuteContext ctx) {
        if (provider.hasCurrent()) {
            long duration = System.currentTimeMillis() - start;
            maybeAddTiming(ctx, duration);
        }
    }

    /**
     * Adds a timing if the context contains a renderable SQL statement. Override to customise filtering.
     *
     * @param ctx the jOOQ execute context
     * @param duration the duration of the execution in milliseconds
     */
    protected void maybeAddTiming(ExecuteContext ctx, long duration) {
        String query = renderInlined(ctx);
        if (query != null) {
            addTiming(ctx, query, duration);
        }
    }

    /**
     * Records the given SQL query and duration in the current profiler session. Override to customise recording.
     *
     * @param ctx the jOOQ execute context
     * @param query the SQL query string to record
     * @param duration the duration of the execution in milliseconds
     */
    @SuppressWarnings("unused")
    protected void addTiming(ExecuteContext ctx, String query, long duration) {
        provider.current().addCustomTiming("sql", "query", query, duration);
    }

}
