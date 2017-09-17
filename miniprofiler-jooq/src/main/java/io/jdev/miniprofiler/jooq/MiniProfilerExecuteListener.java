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
import org.jooq.Configuration;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteType;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.tools.StringUtils;

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
public class MiniProfilerExecuteListener extends DefaultExecuteListener {

    private final ProfilerProvider provider;
    private long start;

    /**
     * Constructs a listener with the given profiler.
     * @param provider the profiler provider to use
     */
    public MiniProfilerExecuteListener(ProfilerProvider provider) {
        this.provider = provider;
    }

    @Override
    public void start(ExecuteContext ctx) {
        start = System.currentTimeMillis();
    }

    @Override
    public void end(ExecuteContext ctx) {
        if (provider.hasCurrent()) {
            long duration = System.currentTimeMillis() - start;
            addTiming(ctx, duration);
        }
    }

    private void addTiming(ExecuteContext ctx, long duration) {
        Configuration configuration = ctx.configuration();

        String query = null;
        if (ctx.query() != null) {
            query = DSL.using(configuration).renderInlined(ctx.query());
        } else if (ctx.routine() != null) {
            query = DSL.using(configuration).renderInlined(ctx.routine());
        } else if (!StringUtils.isBlank(ctx.sql())) {
            query = ctx.sql();
        } else if(ctx.type() == ExecuteType.BATCH) {
            String[] statements = ctx.batchSQL();
            if (statements != null && statements.length > 0) {
                StringBuilder queries = new StringBuilder();
                for (String sql : statements) {
                    if (queries.length() > 0) {
                        queries.append(";\n");
                    }
                    queries.append(sql);
                }
                query = queries.toString();
            }

        }

        if (query != null) {
            provider.current().addCustomTiming("sql", "query", query, duration);
        }
    }

}
