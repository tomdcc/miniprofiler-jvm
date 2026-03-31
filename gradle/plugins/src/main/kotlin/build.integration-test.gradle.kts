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

val suiteName = "integrationTest"
testing {
    suites {
        register<JvmTestSuite>(suiteName) {
            makeBrowserTest(project)
        }
    }
}

configurations {
    extendFromTest(suiteName)
}

tasks.named("check") {
    dependsOn(testing.suites.named(suiteName))
}
