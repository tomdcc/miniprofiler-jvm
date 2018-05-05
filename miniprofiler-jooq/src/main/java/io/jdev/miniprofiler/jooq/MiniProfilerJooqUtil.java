/*
 * Copyright 2018 the original author or authors.
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

import org.jooq.Configuration;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteType;
import org.jooq.impl.DSL;
import org.jooq.tools.StringUtils;

@SuppressWarnings("WeakerAccess")
public final class MiniProfilerJooqUtil {

    public static String renderInlined(ExecuteContext ctx) {
        Configuration configuration = ctx.configuration();
        if (ctx.query() != null) {
            return DSL.using(configuration).renderInlined(ctx.query());
        } else if (ctx.routine() != null) {
            return DSL.using(configuration).renderInlined(ctx.routine());
        } else if (!StringUtils.isBlank(ctx.sql())) {
            return ctx.sql();
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
                return queries.toString();
            }
        }
        return null;
    }

    private MiniProfilerJooqUtil() {
    }
}
