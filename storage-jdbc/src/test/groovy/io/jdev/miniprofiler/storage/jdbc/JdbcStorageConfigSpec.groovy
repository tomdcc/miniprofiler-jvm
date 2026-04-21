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

package io.jdev.miniprofiler.storage.jdbc

import spock.lang.Specification

class JdbcStorageConfigSpec extends Specification {

    def "reads all properties from system properties"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.jdbc.url"]      = "jdbc:h2:mem:test"
        sysprops["miniprofiler.storage.jdbc.username"]  = "sa"
        sysprops["miniprofiler.storage.jdbc.password"]  = "secret"
        sysprops["miniprofiler.storage.jdbc.table"]     = "my_table"
        sysprops["miniprofiler.storage.jdbc.dialect"]   = "h2"
        sysprops["miniprofiler.storage.jdbc.jndiName"]  = "java:comp/env/jdbc/profiler"

        when:
        def config = JdbcStorageConfig.create(sysprops, null)

        then:
        verifyAll(config) {
            url == "jdbc:h2:mem:test"
            username == "sa"
            password == "secret"
            table == "my_table"
            dialect == "h2"
            jndiName == "java:comp/env/jdbc/profiler"
            configured
        }
    }

    def "reads properties from file properties"() {
        given:
        def fileProps = new Properties()
        fileProps["storage.jdbc.url"]      = "jdbc:postgresql://localhost/db"
        fileProps["storage.jdbc.username"]  = "user"

        when:
        def config = JdbcStorageConfig.create(new Properties(), fileProps)

        then:
        config.url == "jdbc:postgresql://localhost/db"
        config.username == "user"
        config.configured
    }

    def "system properties override file properties"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.jdbc.url"] = "jdbc:h2:mem:sys"
        def fileProps = new Properties()
        fileProps["storage.jdbc.url"] = "jdbc:h2:mem:file"

        when:
        def config = JdbcStorageConfig.create(sysprops, fileProps)

        then:
        config.url == "jdbc:h2:mem:sys"
    }

    def "returns unconfigured when neither url or jndiName is set"() {
        when:
        def config = JdbcStorageConfig.create(new Properties(), null)

        then:
        !config.configured
        config.url == null
        config.jndiName == null
    }

    def "is configured when only jndiName is set"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.jdbc.jndiName"] = "java:comp/env/jdbc/ds"

        when:
        def config = JdbcStorageConfig.create(sysprops, null)

        then:
        config.configured
        config.jndiName == "java:comp/env/jdbc/ds"
        config.url == null
    }

    def "null values for optional properties"() {
        given:
        def sysprops = new Properties()
        sysprops["miniprofiler.storage.jdbc.url"] = "jdbc:h2:mem:test"

        when:
        def config = JdbcStorageConfig.create(sysprops, null)

        then:
        verifyAll(config) {
            url == "jdbc:h2:mem:test"
            username == null
            password == null
            table == null
            dialect == null
            jndiName == null
        }
    }
}
