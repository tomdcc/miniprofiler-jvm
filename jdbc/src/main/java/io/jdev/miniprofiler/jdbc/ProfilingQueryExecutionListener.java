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

package io.jdev.miniprofiler.jdbc;

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;

import java.util.List;
import java.util.TreeMap;

/**
 * A {@link QueryExecutionListener} that records SQL query timings in MiniProfiler.
 *
 * <p>For each executed query, records a custom timing of type {@code "sql"} and
 * subtype {@code "query"} against the current {@link Profiler} obtained from the
 * configured {@link ProfilerProvider}. Prepared-statement parameters are interpolated
 * into the SQL text so the recorded command reads as a self-contained statement.</p>
 */
class ProfilingQueryExecutionListener implements QueryExecutionListener {

    private final ProfilerProvider profilerProvider;

    /**
     * Creates a new listener backed by the given profiler provider.
     *
     * @param profilerProvider the provider used to resolve the current profiler for each recorded query; must not be null
     */
    public ProfilingQueryExecutionListener(ProfilerProvider profilerProvider) {
        if (profilerProvider == null) {
            throw new NullPointerException("profilerProvider must not be null");
        }
        this.profilerProvider = profilerProvider;
    }

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        // no-op; timing is recorded in afterQuery using ExecutionInfo.getElapsedTime()
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        Profiler profiler = profilerProvider.current();
        if (!profiler.isActive()) {
            // No live profiling session; skip the (non-trivial) SQL rendering entirely.
            return;
        }
        long elapsed = execInfo.getElapsedTime();
        StringBuilder sql = new StringBuilder();
        for (QueryInfo queryInfo : queryInfoList) {
            if (sql.length() > 0) {
                sql.append("\n\n");
            }
            sql.append(renderSql(queryInfo));
        }
        profiler.addCustomTiming("sql", "query", sql.toString(), elapsed);
    }

    /**
     * Renders the given {@link QueryInfo} as a self-contained SQL string with any
     * positional prepared-statement parameters interpolated in place of {@code ?}.
     *
     * <p>For batched executions only the first batch iteration's parameters are used
     * for interpolation; this matches the existing log4jdbc-based behaviour.</p>
     *
     * @param queryInfo the query info from datasource-proxy
     * @return the SQL text with parameters interpolated, or the raw SQL if no parameters were captured
     */
    private static String renderSql(QueryInfo queryInfo) {
        String sql = queryInfo.getQuery();
        List<List<ParameterSetOperation>> paramsList = queryInfo.getParametersList();
        if (paramsList == null || paramsList.isEmpty()) {
            return sql;
        }
        List<ParameterSetOperation> params = paramsList.get(0);
        if (params == null || params.isEmpty()) {
            return sql;
        }
        TreeMap<Integer, String> byIndex = new TreeMap<Integer, String>();
        for (ParameterSetOperation op : params) {
            if (ParameterSetOperation.isRegisterOutParameterOperation(op)) {
                continue;
            }
            Object[] args = op.getArgs();
            if (args == null || args.length < 1 || !(args[0] instanceof Integer)) {
                // skip named parameters (CallableStatement String-keyed setters); only positional ? markers are interpolated
                continue;
            }
            int index = (Integer) args[0];
            Object value = args.length >= 2 ? args[1] : null;
            if (ParameterSetOperation.isSetNullParameterOperation(op)) {
                value = null;
            }
            byIndex.put(index, formatValue(value));
        }
        if (byIndex.isEmpty()) {
            return sql;
        }
        StringBuilder sb = new StringBuilder(sql.length() + byIndex.size() * 8);
        int paramIdx = 1;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '?' && byIndex.containsKey(paramIdx)) {
                sb.append(byIndex.get(paramIdx));
                paramIdx++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
