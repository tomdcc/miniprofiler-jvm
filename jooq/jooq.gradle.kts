/*
 * Copyright 2017 the original author or authors.
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
    id("build.java-module")
    id("build.publish")
}

dependencies {
	api(projects.core)
	compileOnly(libs.jooq.compile)

    testImplementation(projects.test)
    testImplementation(libs.jooq.test)
    testImplementation(libs.h2)
}

val crossVersionTestJooqV3_0 = addCrossVersionTestSuite("crossVersionTestJooqV3_0", 8) {
    // jOOQ 3.0.0: compile target, minimum supported version (Java 8)
    dependencies {
        implementation(libs.jooq.v300)
        implementation(libs.h2)
    }
}
addCrossVersionTestSuite("crossVersionTestJooqV3_14", 8) {
    // jOOQ 3.14.16: last Java 8 version
    dependencies {
        implementation(libs.jooq.v314)
        implementation(libs.h2)
    }
}
addCrossVersionTestSuite("crossVersionTestJooqV3_16", 11) {
    // jOOQ 3.16.10: last Java 11 version
    dependencies {
        implementation(libs.jooq.v316)
        implementation(libs.h2)
    }
}
addCrossVersionTestSuite("crossVersionTestJooqV3_19", 17) {
    // jOOQ 3.19.31: last Java 17 version (last open-source before Java 21 was required)
    dependencies {
        implementation(libs.jooq.v319)
        implementation(libs.h2)
    }
}
addCrossVersionTestSuite("crossVersionTestJooqV3_21", 21) {
    // jOOQ 3.21.1: latest version (Java 21)
    dependencies {
        implementation(libs.jooq.v321)
        implementation(libs.h2)
    }
}

// The v3_0 suite tests the minimum supported version and runs as part of regular check
tasks.named("check") { dependsOn(crossVersionTestJooqV3_0) }

// Force each suite to use its specific jOOQ version, overriding the 3.14.16 inherited
// from testImplementation and the 3.0.0 compile-only dependency
listOf("CompileClasspath", "RuntimeClasspath").forEach { suffix ->
    configurations.named("crossVersionTestJooqV3_0$suffix") {
        resolutionStrategy.force("org.jooq:jooq:3.0.0")
    }
    configurations.named("crossVersionTestJooqV3_14$suffix") {
        resolutionStrategy.force("org.jooq:jooq:3.14.16")
    }
    configurations.named("crossVersionTestJooqV3_16$suffix") {
        resolutionStrategy.force("org.jooq:jooq:3.16.10")
    }
    configurations.named("crossVersionTestJooqV3_19$suffix") {
        resolutionStrategy.force("org.jooq:jooq:3.19.31")
    }
    configurations.named("crossVersionTestJooqV3_21$suffix") {
        resolutionStrategy.force("org.jooq:jooq:3.21.1")
    }
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler jOOQ Support"
            description = "Support classes for getting SQL statements profiled in the MiniProfiler using jOOQ."
        }
    }
}
