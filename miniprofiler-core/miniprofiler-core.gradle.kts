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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    alias(libs.plugins.shadow)
    id("build.java-module")
    id("build.publish")
}

val bundled by configurations.creating { }

project.sourceSets.configureEach { ->
    compileClasspath += configurations["bundled"]
    runtimeClasspath += configurations["bundled"]
}

dependencies {
    compileOnly(libs.servlet.api)
    compileOnly(libs.ehcache) {
        isTransitive = false
    }

	// for profiling eclipselink-generated sql
    compileOnly(libs.eclipselink) {
        isTransitive = false
    }

	// for profiling hibernate-generated sql
    compileOnly(libs.hibernate) {
        isTransitive = false
    }

    bundled(libs.json.simple) {
        exclude(group = "junit", module = "junit")
    }

    testImplementation(libs.jackson.databind)
    testRuntimeOnly(libs.slf4j.api)

	// needed since we're excluding ehcache's deps and spock falls over otherwise
    testRuntimeOnly(libs.jta)
}

tasks.named<ProcessResources>("processResources") {
	filesMatching("**/miniprofiler-version.txt") {
		filter(mapOf("version" to version), ReplaceTokens::class.java)
	}
    inputs.property("version", version)
}


// include hibernate builder source, since it's LGPL and we don't want to add any
// extra burden on people who might be bundling this library in
tasks.named<Jar>("jar").configure {
	from(sourceSets["main"].allJava) {
        include("io/jdev/miniprofiler/sql/hibernate/*")
    }
    archiveClassifier.set("plain")
}

// remove plain jar from publishing
listOf(configurations["runtimeElements"], configurations["apiElements"]).forEach { conf ->
    conf.outgoing {
        artifacts.clear()
        artifact(tasks.named("shadowJar"))
    }
}

tasks.named<ShadowJar>("shadowJar").configure {
    archiveClassifier.set("")
    configurations = listOf(project.configurations["bundled"])
    relocate("org.json.simple", "io.jdev.miniprofiler.shadowed.org.json.simple")
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler JVM Core"
            description = "MiniProfiler JVM is an implementation of the StackExchange MiniProfiler for the JVM. It provides easily accessible profiling information while developing a web-based app."
        }
    }
}

project.tasks.withType<Test>().configureEach {
    systemProperty("test.miniprofiler.version", project.version)
}
