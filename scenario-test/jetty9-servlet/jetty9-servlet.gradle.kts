/*
 * Copyright 2013-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("build.docker-test")
    id("build.scenario-test")
    id("build.java-module")
}

dependencies {
    compileOnly(libs.javax.ee.api)
    implementation(projects.core)
    implementation(projects.jdbc)
    // needs to be a jar to pick up tld automatically
    implementation(projects.javaxServlet) {
        targetConfiguration = "jars"
    }
    implementation(libs.h2)

    scenarioTestImplementation(projects.testlibIntegration)
    scenarioTestRuntimeOnly(scenarioTestFixtures(projects.javaxServlet))
}

tasks.named<Test>("scenarioTest").configure {
    systemProperty("dockerImage.jetty9", images.versions.jetty9.get())
    val warFile = tasks.named<War>("war").flatMap { it.archiveFile }
    doFirst {
        systemProperty("scenarioTest.warPath", warFile.get().asFile.absolutePath)
    }
}
