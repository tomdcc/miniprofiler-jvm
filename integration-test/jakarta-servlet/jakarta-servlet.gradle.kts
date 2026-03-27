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

plugins {
    id("build.docker-test")
    id("build.integration-test")
    id("build.java-module")
}

// jakarta.servlet-api 6.0 requires Java 11+
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

// build.docker-test adds compileOnly(libs.javaee) — exclude it and use jakarta instead
configurations.named("compileOnly") {
    exclude(group = "javax", module = "javaee-api")
}

dependencies {
    compileOnly(libs.jakarta.servlet.api)
    implementation(projects.miniprofilerCore)
    // needs to be a jar to pick up tld automatically
    implementation(projects.miniprofilerJakartaServlet) {
        targetConfiguration = "jars"
    }
    implementation(libs.h2)

    integrationTestImplementation(projects.integrationTest.lib)
}

tasks.named<Test>("integrationTest").configure {
    val warFile = tasks.named<War>("war").flatMap { it.archiveFile }
    doFirst {
        systemProperty("integrationTest.warPath", warFile.get().asFile.absolutePath)
    }
}
