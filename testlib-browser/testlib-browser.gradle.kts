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

plugins {
    id("build.java-module")
}

// Geb 8.x / Selenium 4.x / test-geb-groovy4 all require Java 11+
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

dependencies {
    // Core types: TestedServer, BaseProfilerProvider, Storage
    api(projects.core)
    // TestedServer
    api(projects.testlibIntegration)

    // Geb page objects: compileOnly because the browser test runtime provides them via makeBrowserTest
    compileOnly(projects.testGebGroovy4)
    compileOnly(libs.groovy.v4)
    compileOnly(libs.geb.core.groovy4)
    compileOnly(libs.geb.spock.groovy4)
    compileOnly(libs.spock.groovy4)
}
