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
    id("build.base")
    id("build.build-parameters")
    id("build.java-module")
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
}

dependencies {
    api(projects.test)
    compileOnly(libs.groovy.v3)
    compileOnly(libs.geb.core.groovy3)
    compileOnly(libs.selenium.api.groovy3)
}

// Cannot use build.browser-test here because it hardcodes groovy4 Geb deps;
// this module needs the groovy3 variants instead.
val browserTestSuite = addTestSuite("browserTest", 11) {
    makeBrowserTest(project, "groovy3")
}

sourceSets.named("browserTest") {
    groovy.srcDir("../test-geb/src/browserTest/groovy")
    resources.srcDir("../test-geb/src/browserTest/resources")
}

dependencies {
    "browserTestImplementation"(projects.core)
    "browserTestImplementation"(libs.bundles.testing.groovy3)
}

// build.java-module adds org.apache.groovy (groovy.v4) — exclude all of it so only groovy.v3 is on the browserTest classpath
configurations.named("browserTestImplementation") {
    exclude(group = "org.apache.groovy")
}
// Force spock.groovy3 to win over spock.groovy4 that build.java-module adds
configurations.matching { it.name.startsWith("browserTest") }.configureEach {
    resolutionStrategy {
        force(libs.spock.groovy3.get())
    }
}

tasks.named("fullCheck") {
    dependsOn(browserTestSuite)
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Geb Test Support (Groovy 3)"
            description = "Geb modules and test utilities for verifying the MiniProfiler UI in browser-based functional tests (Groovy 3 variant)"
        }
    }
}
