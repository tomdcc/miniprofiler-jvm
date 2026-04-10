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
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("build.base")
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
    testImplementation(libs.groovy.v4)
    testImplementation(dependencies.create(libs.spock.groovy4.get()) as ModuleDependency) {
        exclude(group = "org.apache.groovy", module = "groovy-all")
    }
    testImplementation(libs.junit.jupiter)
    testImplementation(dependencies.create(libs.awaitility.get()) as ModuleDependency) {
        exclude(group = "org.codehaus.groovy", module = "groovy")
        exclude(group = "org.apache.groovy", module = "groovy")
    }
    testRuntimeOnly(libs.byte.buddy)
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

// When both org.codehaus.groovy and org.apache.groovy are on the classpath (e.g. via ratpack-groovy
// which uses Groovy 2.5), Groovy 4 declares capability equivalence and causes a conflict.
// Resolve by selecting the highest version (Groovy 4) as the winner.
configurations.all {
    resolutionStrategy.capabilitiesResolution.all {
        if (candidates.any { (it.id as? ModuleComponentIdentifier)?.group == "org.apache.groovy" }) {
            selectHighestVersion()
        }
    }
}

if (!project.path.startsWith(":scenario-test:")) {
    sourceSets {
        test {
            compileClasspath += sourceSets.main.get().compileClasspath
            runtimeClasspath += sourceSets.main.get().compileClasspath
        }
    }
}

tasks.named("sanityCheck") {
    dependsOn(tasks.withType<AbstractCompile>())
    dependsOn(tasks.withType<Checkstyle>())
    dependsOn(tasks.withType<CodeNarc>())
    dependsOn(tasks.withType<Javadoc>())
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing/private")
}

tasks.named("fullCheck") {
    dependsOn(tasks.named("check"))
}
