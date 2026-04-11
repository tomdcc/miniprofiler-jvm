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
    id("build.browser-test")
    id("build.groovy-module")
    id("build.publish")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

sourceSets {
    main {
        groovy.srcDir("../test-geb/src/main/groovy")
        resources.srcDir("../test-geb/src/main/resources")
    }
    browserTest {
        groovy.srcDir("../test-geb/src/browserTest/groovy")
        resources.srcDir("../test-geb/src/browserTest/resources")
    }
}

dependencies {
    api(projects.test)
    compileOnly(libs.groovy.v4)
    compileOnly(libs.geb.core.groovy4)
    compileOnly(libs.selenium.api.groovy4)

    browserTestImplementation(projects.core)
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Geb Test Support (Groovy 4)"
            description = "Geb modules and test utilities for verifying the MiniProfiler UI in browser-based functional tests (Groovy 4 variant)"
        }
    }
}
