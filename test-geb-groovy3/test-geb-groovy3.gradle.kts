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
    id("build.publish")
}

sourceSets {
    main {
        groovy.srcDir("../test-geb/src/main/groovy")
        resources.srcDir("../test-geb/src/main/resources")
    }
    test {
        groovy.srcDir("../test-geb/src/test/groovy")
        resources.srcDir("../test-geb/src/test/resources")
    }
}

dependencies {
    api(projects.test)
    compileOnly(libs.groovy.v3)
    compileOnly(libs.geb.core.groovy3)
    compileOnly(libs.selenium.api.groovy3)

    // Override build.java-module's groovy.v4 + spock.groovy4 with Groovy 3 equivalents
    testImplementation(libs.groovy.v3)
    testImplementation(libs.spock.groovy3)
}

// build.java-module adds org.apache.groovy (groovy.v4) — exclude all of it so only groovy.v3 is on the test classpath
configurations.named("testImplementation") {
    exclude(group = "org.apache.groovy")
}
// Force spock.groovy3 to win over spock.groovy4 that build.java-module adds
configurations.matching { it.name.startsWith("test") }.configureEach {
    resolutionStrategy {
        force(libs.spock.groovy3.get())
    }
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            makeBrowserTest(project, "groovy3")
        }
    }
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
