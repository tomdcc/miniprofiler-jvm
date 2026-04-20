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
    id("build.integration-test")
    id("build.publish")
}

dependencies {
    api(projects.core)

    implementation(libs.hikaricp)

    // JDBC drivers declared compileOnly — users bring their own versions.
    compileOnly(libs.h2)
    compileOnly(libs.postgresql)
    compileOnly(libs.mysql.connector)
    compileOnly(libs.mssql.jdbc)
    compileOnly(libs.oracle.jdbc)

    // compileOnly deps are not inherited by integrationTest suite — re-declare them.
    integrationTestImplementation(libs.h2)
    integrationTestImplementation(libs.postgresql)
    integrationTestImplementation(libs.mysql.connector)
    integrationTestImplementation(libs.mssql.jdbc)
    integrationTestImplementation(libs.oracle.jdbc)
    integrationTestImplementation(libs.testcontainers.core)
    integrationTestImplementation(libs.testcontainers.junit5)
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
