/*
 * Copyright 2014-2026 the original author or authors.
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
    id("build.cross-version-test")
    id("build.java-module")
    id("build.publish")
}

java {
    toolchain {
        // EclipseLink 4+ class files require a Java 11 compiler to read
        languageVersion = JavaLanguageVersion.of(11)
    }
}

tasks.withType<JavaCompile>().matching { it.name == "compileJava" }.configureEach {
    // Output Java 8 bytecode so the module can deploy to Java 8 containers (e.g. GlassFish 4)
    options.release = 8
}

// Compiled against EclipseLink 4.x which has both the legacy config.SessionCustomizer and
// the current sessions.SessionCustomizer, allowing ProfilingSessionCustomizer and
// LegacyProfilingSessionCustomizer to coexist in the same compilation unit.
dependencies {
    api(projects.core)
    api(projects.jdbc)
    compileOnly(libs.eclipselink.v4) {
        isTransitive = false
    }
    testImplementation(projects.test)
    testImplementation(libs.eclipselink.v4) {
        isTransitive = false
    }
}

crossVersionTests {
    configureEach {
        implementation(libs.bundles.testing.groovy4)
        runtimeOnly(libs.bundles.testing.runtime)
        implementation(projects.test)
        implementation(projects.jdbc)
        implementation(libs.h2)
    }
    register("crossVersionTestEclipseLink2") {
        minJavaVersion = 8
        implementation(libs.eclipselink.v2)
    }
    register("crossVersionTestEclipseLink3") {
        minJavaVersion = 11
        implementation(libs.eclipselink.v3)
    }
    register("crossVersionTestEclipseLink4") {
        minJavaVersion = 11
        implementation(libs.eclipselink.v4)
    }
    register("crossVersionTestEclipseLink5") {
        minJavaVersion = 17
        implementation(libs.eclipselink.v5)
    }
}

tasks.named("check") { dependsOn("crossVersionTestEclipseLink4") }

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler EclipseLink Module"
            description = "EclipseLink SQL profiling support for MiniProfiler."
        }
    }
}
