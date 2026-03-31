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
        groovy.srcDir("../miniprofiler-test-geb/src/main/groovy")
        resources.srcDir("../miniprofiler-test-geb/src/main/resources")
    }
    test {
        groovy.srcDir("../miniprofiler-test-geb/src/test/groovy")
        resources.srcDir("../miniprofiler-test-geb/src/test/resources")
    }
}

dependencies {
    api(projects.miniprofilerTest)
    compileOnly(libs.groovy.v4)
    compileOnly(libs.geb.core.v4)
    compileOnly(libs.selenium.api)
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            makeBrowserTest(project)
        }
    }
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
