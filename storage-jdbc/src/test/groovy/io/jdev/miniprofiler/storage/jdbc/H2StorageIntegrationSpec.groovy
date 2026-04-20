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

import io.jdev.miniprofiler.storage.jdbc.dialect.H2Dialect
import org.h2.jdbcx.JdbcDataSource
import spock.lang.Shared

class H2StorageIntegrationSpec extends BaseJdbcStorageIntegrationSpec {

    @Shared JdbcStorage h2Storage

    JdbcStorage getStorage() { h2Storage }

    void setupSpec() {
        def dataSource = new JdbcDataSource()
        dataSource.url = "jdbc:h2:mem:integration-test;DB_CLOSE_DELAY=-1"
        h2Storage = new JdbcStorage(dataSource, new H2Dialect())
        h2Storage.createTable()
    }

    void cleanupSpec() {
        h2Storage?.close()
    }
}
