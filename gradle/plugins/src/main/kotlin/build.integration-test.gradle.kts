/*
 * Copyright 2026 the original author or authors.
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

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the

plugins {
    id("build.build-parameters")
    id("java-library")
}

val libs = the<LibrariesForLibs>()

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                // type-safe project accessors are not available in precompiled script plugins
                implementation(project(":miniprofiler-test-support"))
                implementation(libs.geb.core)
                implementation(libs.geb.spock)
                implementation(libs.selenium.api)
                runtimeOnly(libs.selenium.support)
                runtimeOnly(libs.selenium.firefox.driver)
            }
            targets {
                all {
                    testTask.configure {
                        systemProperty("geb.build.reportsDir", "${reporting.baseDirectory.get().asFile}/geb")
                        buildParameters.browserTest.firefoxBinPath.orNull?.let {
                            systemProperty("webdriver.firefox.bin", it)
                        }
                    }
                }
            }
        }
    }
}

// Inherit unit test dependencies (Spock, Groovy, JUnit Platform, etc.) from the build.java-module
// convention plugin so they don't need to be redeclared for integration tests.
configurations {
    named("integrationTestImplementation") {
        extendsFrom(configurations.testImplementation.get())
    }
    named("integrationTestRuntimeOnly") {
        extendsFrom(configurations.testRuntimeOnly.get())
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
