/*
 * Copyright 2026 the original author or authors.
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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
    id("build.integration-test")
    id("build.java-module")
    id("build.publish")
    id("application")
    id("java-test-fixtures")
}

application.mainClass = "io.jdev.miniprofiler.viewer.MiniProfilerViewerMain"

dependencies {
    implementation(projects.miniprofilerCore)
}

dependencies {
    testFixturesImplementation(libs.groovy.v4)
}

val testFixturesResourceDir = file("src/testFixtures/resources").absolutePath

tasks.withType<Test>().configureEach {
    systemProperty("miniprofiler.viewer.testFixturesDir", testFixturesResourceDir)
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("original")
}

tasks.named<Test>("integrationTest") {
    dependsOn(shadowJarTask)
    systemProperty("miniprofiler.viewer.jar", shadowJarTask.get().archiveFile.get().asFile.absolutePath)
}

publishing {
    publications.named<MavenPublication>("maven") {
        artifact(shadowJarTask)
        artifact(tasks.named("sourcesJar"))
        artifact(tasks.named("javadocJar"))
        pom {
            name = "MiniProfiler Standalone Viewer"
            description = "Standalone app to allow viewing saved profiles."
        }
    }
}
