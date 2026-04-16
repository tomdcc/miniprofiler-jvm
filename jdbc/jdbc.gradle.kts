/*
 * Copyright 2026 the original author or authors.
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
    api(projects.core)
    bundled(libs.datasource.proxy)

    testImplementation(projects.test)
    testImplementation(libs.h2)
}

tasks.named<Jar>("jar").configure {
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
    relocate("net.ttddyy.dsproxy", "io.jdev.miniprofiler.jdbc.shadowed.net.ttddyy.dsproxy")
    // exclude the datasource-proxy JSP tag library descriptor — it references
    // javax.servlet.jsp classes that break Jakarta EE containers like GlassFish 7
    exclude("META-INF/dsproxy.tld")

    // include license for the bundled dependency
    from("licenses/datasource-proxy-LICENSE.txt") {
        into("META-INF/licenses/datasource-proxy")
        rename { "LICENSE" }
    }
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler JDBC Module"
            description = "JDBC query profiling for MiniProfiler using datasource-proxy."
        }
    }
}
