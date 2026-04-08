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
    id("java-library")
    id("war")
}

val libs = the<LibrariesForLibs>()

dependencies {
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit5)
}

tasks.withType<Test>().configureEach {
    // Also set DOCKER_API_VERSION so docker-java negotiates with a version Docker 29+ accepts
    // (Docker Desktop 29.x rejects client requests below API 1.44).
    environment("DOCKER_API_VERSION", "1.45")

    // The WAR must be built before tests run so the LauncherSessionListener
    // can copy it into the container.
    dependsOn(tasks.named("assemble"))
}
