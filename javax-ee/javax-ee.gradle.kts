/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("build.java-module")
    id("build.publish")
    id("build.scenario-test-fixtures")
    id("build.integration-test")
}

dependencies {
	api(projects.core)
    compileOnly(libs.javax.ee.api)

    testImplementation(projects.test)
    testImplementation(libs.javax.ee.api)
    testRuntimeOnly(projects.javaxServlet)

    integrationTestImplementation(libs.weld.se.v3)

    scenarioTestFixturesImplementation(libs.groovy.v4)
    scenarioTestFixturesImplementation(libs.junit.platform.launcher)
    scenarioTestFixturesImplementation(projects.testlibIntegration)
    scenarioTestFixturesImplementation(libs.testcontainers.core)
}

configurations.named("integrationTestRuntimeClasspath") {
    exclude(group = "javax", module = "javaee-api")
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Javax EE Module"
            description = "Support classes for getting the MiniProfiler working out of the box in modern Java EE containers."
        }
    }
}
