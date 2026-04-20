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
}

val libs = the<LibrariesForLibs>()
val suiteName = "containerTest"

val containerTestSuite = addTestSuite(suiteName, 11) {
    dependencies {
        implementation(sourceSets["main"].output)
        implementation(libs.testcontainers.core)
        implementation(libs.testcontainers.junit5)
    }
    targets.configureEach {
        testTask.configure {
            // Also set DOCKER_API_VERSION so docker-java negotiates with a version Docker 29+ accepts
            // (Docker Desktop 29.x rejects client requests below API 1.44).
            environment("DOCKER_API_VERSION", "1.45")
            // Each spec manages its own container; run them in parallel across forks.
            maxParallelForks = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
        }
    }
}

// NOTE: intentionally NOT wired into the check task — run explicitly or via fullCheck
tasks.named("fullCheck") {
    dependsOn(containerTestSuite)
}
