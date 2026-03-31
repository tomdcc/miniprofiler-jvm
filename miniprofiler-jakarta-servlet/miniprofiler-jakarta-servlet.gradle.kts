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
    id("build.java-module")
    id("build.publish")
}

// Spring 6 test dependency requires Java 17+; production code targets Java 11+
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.release = 11
}

dependencies {
    api(projects.miniprofilerCore)
    compileOnly(libs.jakarta.servlet.api)
    compileOnly(libs.jakarta.jsp.api)
    testImplementation(projects.miniprofilerTest)
    testImplementation(libs.spring.v6.test)
    testImplementation(libs.spring.v6.web)
}

// to allow deps on a jar, so that a tld will get picked up
val jars by configurations.registering { }
artifacts.add("jars", tasks.named("jar"))

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Jakarta Servlet Module"
            description = "Support classes for getting the MiniProfiler working in jakarta.servlet environments."
        }
    }
}
