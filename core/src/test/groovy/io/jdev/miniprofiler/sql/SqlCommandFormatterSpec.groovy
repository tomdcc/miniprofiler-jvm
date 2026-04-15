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

    void "honours sql.format.indent.width from miniprofiler.properties"() {
        given:
        def props = new Properties()
        props.setProperty("sql.format.indent.width", "4")
        def configured = new SqlCommandFormatter(new Properties(), props)

        when:
        def result = configured.format('select * from "foo"')

        then:
        result == 'select\n    *\nfrom\n    "foo"'
    }

    void "honours sql.format.uppercase from miniprofiler.properties"() {
        given:
        def props = new Properties()
        props.setProperty("sql.format.uppercase", "true")
        def configured = new SqlCommandFormatter(new Properties(), props)

        when:
        def result = configured.format('select * from "foo"')

        then:
        result == 'SELECT\n  *\nFROM\n  "foo"'
    }

    void "honours sql.format.dialect from miniprofiler.properties (case-insensitive)"() {
        given:
        def props = new Properties()
        // lowercase value exercises ConfigHelper's case-insensitive enum lookup
        props.setProperty("sql.format.dialect", "postgresql")

        expect:
        new SqlCommandFormatter(new Properties(), props) != null
    }

    void "rejects unknown sql.format.dialect"() {
        given:
        def props = new Properties()
        props.setProperty("sql.format.dialect", "NotARealDialect")

        when:
        new SqlCommandFormatter(new Properties(), props)

        then:
        thrown(IllegalArgumentException)
    }

    void "system properties override miniprofiler.properties"() {
        given:
        def fileProps = new Properties()
        fileProps.setProperty("sql.format.uppercase", "false")
        def systemProps = new Properties()
        systemProps.setProperty("miniprofiler.sql.format.uppercase", "true")
        def configured = new SqlCommandFormatter(systemProps, fileProps)

        when:
        def result = configured.format('select * from "foo"')

        then:
        result.startsWith("SELECT")
    }
}
