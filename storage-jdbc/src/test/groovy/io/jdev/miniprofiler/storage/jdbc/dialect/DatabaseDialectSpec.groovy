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

package io.jdev.miniprofiler.storage.jdbc.dialect

import spock.lang.Specification

class DatabaseDialectSpec extends Specification {

    def "detect returns H2Dialect for H2 URL"() {
        expect:
        DatabaseDialect.detect("jdbc:h2:mem:test") instanceof H2Dialect
    }

    def "detect returns PostgresDialect for PostgreSQL URL"() {
        expect:
        DatabaseDialect.detect("jdbc:postgresql://localhost/mydb") instanceof PostgresDialect
    }

    def "detect returns MysqlDialect for MySQL URL"() {
        expect:
        DatabaseDialect.detect("jdbc:mysql://localhost/mydb") instanceof MysqlDialect
    }

    def "detect returns MysqlDialect for MariaDB URL"() {
        expect:
        DatabaseDialect.detect("jdbc:mariadb://localhost/mydb") instanceof MysqlDialect
    }

    def "detect returns MssqlDialect for SQL Server URL"() {
        expect:
        DatabaseDialect.detect("jdbc:sqlserver://localhost;databaseName=mydb") instanceof MssqlDialect
    }

    def "detect throws for unknown URL"() {
        when:
        DatabaseDialect.detect("jdbc:unknown:foo")

        then:
        thrown(IllegalArgumentException)
    }

    def "detect throws for null URL"() {
        when:
        DatabaseDialect.detect(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "forName returns H2Dialect"() {
        expect:
        DatabaseDialect.forName("h2") instanceof H2Dialect
    }

    def "forName returns PostgresDialect"() {
        expect:
        DatabaseDialect.forName("postgresql") instanceof PostgresDialect
    }

    def "forName returns MysqlDialect"() {
        expect:
        DatabaseDialect.forName("mysql") instanceof MysqlDialect
    }

    def "forName returns MssqlDialect"() {
        expect:
        DatabaseDialect.forName("mssql") instanceof MssqlDialect
    }

    def "forName is case-insensitive"() {
        expect:
        DatabaseDialect.forName("H2") instanceof H2Dialect
        DatabaseDialect.forName("POSTGRESQL") instanceof PostgresDialect
        DatabaseDialect.forName("MYSQL") instanceof MysqlDialect
        DatabaseDialect.forName("MSSQL") instanceof MssqlDialect
    }

    def "forName throws for unknown name"() {
        when:
        DatabaseDialect.forName("nosuchdb")

        then:
        thrown(IllegalArgumentException)
    }
}
