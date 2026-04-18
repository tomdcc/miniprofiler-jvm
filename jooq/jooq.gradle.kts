/*
 * Copyright 2018-2026 the original author or authors.
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
    id("build.cross-version-test")
    id("build.java-module")
    id("build.publish")
}

dependencies {
	api(projects.core)
	compileOnly(libs.jooq.v300)

    testImplementation(projects.test)
    testImplementation(libs.jooq.v314)
    testImplementation(libs.h2)
}

crossVersionTests {
    configureEach {
        implementation(libs.bundles.testing.groovy4)
        runtimeOnly(libs.bundles.testing.runtime)
        implementation(projects.test)
        implementation(libs.h2)
    }
    register("crossVersionTestJooqV3_0") {
        // jOOQ 3.0.0: compile target, minimum supported version (Java 8)
        minJavaVersion = 8
        implementation(libs.jooq.v300)
    }
    register("crossVersionTestJooqV3_14") {
        // jOOQ 3.14.16: last Java 8 version
        minJavaVersion = 8
        implementation(libs.jooq.v314)
    }
    register("crossVersionTestJooqV3_16") {
        // jOOQ 3.16.10: last Java 11 version
        minJavaVersion = 11
        implementation(libs.jooq.v316)
    }
    register("crossVersionTestJooqV3_19") {
        // jOOQ 3.19.31: last Java 17 version (last open-source before Java 21 was required)
        minJavaVersion = 17
        implementation(libs.jooq.v319)
    }
    register("crossVersionTestJooqV3_21") {
        // jOOQ 3.21.1: latest version (Java 21)
        minJavaVersion = 21
        implementation(libs.jooq.v321)
    }
}

// The v3_0 suite tests the minimum supported version and runs as part of regular check
tasks.named("check") { dependsOn("crossVersionTestJooqV3_0") }

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler jOOQ Support"
            description = "Support classes for getting SQL statements profiled in the MiniProfiler using jOOQ."
        }
    }
}
