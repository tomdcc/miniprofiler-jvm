/*
 * Copyright 2015-2026 the original author or authors.
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
    id("build.browser-test")
    id("build.integration-test")
    id("build.java-module")
    id("build.publish")
    id("build.ratpack")
    id("java-test-fixtures")
}

dependencies {
    api(projects.core)
    api(projects.jdbc)
    compileOnly(libs.ratpack.core)
    compileOnly(libs.ratpack.guice)
    compileOnly(libs.ratpack.hikari)
    compileOnly(libs.ratpack.h2)

    testImplementation(libs.ratpack.core)
    testImplementation(libs.ratpack.guice)
    testImplementation(libs.ratpack.hikari)
    testImplementation(libs.ratpack.h2)
    testImplementation(projects.test)
    testImplementation(libs.ratpack.test)
    testImplementation(libs.ratpack.groovy.test)

    integrationTestImplementation(projects.core)
    integrationTestImplementation(projects.testlibIntegration)
    integrationTestImplementation(testFixtures(project))
    integrationTestImplementation(libs.ratpack.core)
    integrationTestImplementation(libs.ratpack.guice)

    testFixturesImplementation(projects.testlibIntegration)
    testFixturesImplementation(libs.groovy.v4)
    testFixturesImplementation(libs.ratpack.core)
    testFixturesImplementation(libs.ratpack.guice)

    browserTestImplementation(libs.ratpack.core)
    browserTestImplementation(libs.ratpack.guice)
}
publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Ratpack Support"
            description = "Support classes for getting the MiniProfiler working out of the box in Ratpack."
        }
    }
}
