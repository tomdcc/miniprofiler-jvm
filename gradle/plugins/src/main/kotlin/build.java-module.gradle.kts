/*
 * Copyright 2023 the original author or authors.
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
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("build.checkstyle")
    id("build.codenarc")
    id("groovy")
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

repositories {
    mavenCentral()
}

val libs = the<LibrariesForLibs>()

dependencies {
    testImplementation(libs.groovy)
    testImplementation(dependencies.create(libs.spock.get()) as ModuleDependency) {
        exclude(group = "org.codehaus.groovy", module = "groovy-all")
    }
    testImplementation(libs.junit.jupiter)
    testImplementation(dependencies.create(libs.awaitility.get()) as ModuleDependency) {
        exclude(group = "org.codehaus.groovy", module = "groovy")
    }
    testRuntimeOnly(libs.logback)
}

project.tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }

    // Always run tests in the IDE
    if (System.getProperty("idea.active").toBoolean()) {
        outputs.upToDateWhen { false }
        outputs.doNotCacheIf("Running in IDE") { true }
    }
}

if (!project.path.startsWith(":integration-test:")) {
    sourceSets {
        test {
            compileClasspath += sourceSets.main.get().compileClasspath
            runtimeClasspath += sourceSets.main.get().compileClasspath
        }
    }
}
