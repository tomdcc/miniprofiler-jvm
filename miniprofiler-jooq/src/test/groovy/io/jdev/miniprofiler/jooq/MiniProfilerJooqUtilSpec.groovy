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

package io.jdev.miniprofiler.jooq

import org.jooq.ExecuteContext
import org.jooq.ExecuteType
import org.jooq.Query
import spock.lang.Specification

class MiniProfilerJooqUtilSpec extends Specification {

    void "uses batch SQL when type is BATCH even when ctx.query() is non-null"() {
        given:
        def ctx = Mock(ExecuteContext)
        ctx.type() >> ExecuteType.BATCH
        ctx.query() >> Mock(Query)
        ctx.batchSQL() >> (["insert into foo values (1, 'bar')", "insert into foo values (2, 'baz')"] as String[])

        when:
        def result = MiniProfilerJooqUtil.renderInlined(ctx)

        then:
        result == "insert into foo values (1, 'bar');\ninsert into foo values (2, 'baz')"
    }

    void "returns null when batch SQL is empty"() {
        given:
        def ctx = Mock(ExecuteContext)
        ctx.type() >> ExecuteType.BATCH
        ctx.batchSQL() >> ([] as String[])

        when:
        def result = MiniProfilerJooqUtil.renderInlined(ctx)

        then:
        result == null
    }
}
