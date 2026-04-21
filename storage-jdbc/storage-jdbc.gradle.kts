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

plugins {
    id("build.java-module")
    id("build.container-test")
    id("build.publish")
    `java-test-fixtures`
}

dependencies {
    api(projects.core)

    // HikariCP is optional — users who supply their own DataSource don't need it.
    // The auto-discovery path in JdbcStorageLocator prefers Hikari when available
    // and falls back to a DriverManager-backed DataSource when it is not.
    compileOnly(libs.hikaricp.v4)

    // JDBC drivers declared compileOnly — users bring their own versions.
    compileOnly(libs.h2)
    compileOnly(libs.postgresql)
    compileOnly(libs.mysql.connector)
    compileOnly(libs.mssql.jdbc)
    compileOnly(libs.oracle.jdbc)

    // H2 is used in unit tests for the fast check path.
    testImplementation(libs.h2)
    // Default test suite exercises the Hikari-preferred happy path.
    testImplementation(libs.hikaricp.v4)

    // testFixtures holds the base integration spec — it needs Spock/Groovy.
    testFixturesImplementation(libs.h2)
    testFixturesImplementation(libs.groovy.v4)
    testFixturesImplementation(libs.spock.groovy4)


    // compileOnly deps are not inherited by containerTest suite — re-declare them.
    containerTestImplementation(testFixtures(project))
    containerTestImplementation(libs.h2)
    containerTestImplementation(libs.hikaricp.v4)
    containerTestImplementation(libs.postgresql)
    containerTestImplementation(libs.mysql.connector)
    containerTestImplementation(libs.mssql.jdbc)
    containerTestImplementation(libs.oracle.jdbc)
}

tasks.named<Test>("containerTest").configure {
    systemProperty("dockerImage.postgres", images.versions.postgres.get())
    systemProperty("dockerImage.mysql", images.versions.mysql.get())
    systemProperty("dockerImage.mssql-server", images.versions.mssql.server.get())
    systemProperty("dockerImage.oracle-free", images.versions.oracle.free.get())
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler JDBC Storage"
            description = "JDBC/relational database storage backend for MiniProfiler JVM."
        }
    }
}
