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

package io.jdev.miniprofiler.sql

import spock.lang.Specification

class SqlCommandFormatterSpec extends Specification {

    def formatter = new SqlCommandFormatter()

    void "supports sql type"() {
        expect:
        formatter.supports("sql")
    }

    void "does not support non-sql types"() {
        expect:
        !formatter.supports("http")
        !formatter.supports("memcache")
    }

    void "formats SQL statements"() {
        when:
        def result = formatter.format("select id, name from users where active = 1")

        then:
        result.contains("select")
        result.contains("from")
        result != "select id, name from users where active = 1"
    }
}
