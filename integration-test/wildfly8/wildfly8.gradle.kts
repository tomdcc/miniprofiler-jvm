/*
 * Copyright 2018 the original author or authors.
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
    id("build.docker-test")
    id("build.java-module")
}

dependencies {
    implementation(projects.miniprofilerJavaee)
    implementation(projects.miniprofilerServlet)

    testImplementation(projects.integrationTest.lib)
}

val h2Classpath by configurations.creating
dependencies {
    h2Classpath(libs.h2)
}

tasks.withType<Test>().configureEach {
    val warFile = tasks.named<War>("war").flatMap { it.archiveFile }
    doFirst {
        systemProperty("integrationTest.warPath", warFile.get().asFile.absolutePath)
        systemProperty("integrationTest.h2JarPath", h2Classpath.singleFile.absolutePath)
    }
}
