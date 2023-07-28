import java.awt.Desktop.getDesktop

/*
 * Copyright 2015 the original author or authors.
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
    alias(libs.plugins.asciidoctor)
    id("build.publish")
}

repositories {
    mavenCentral()
}

tasks.asciidoctor.configure {
    baseDirFollowsSourceDir()
}

val api by tasks.registering(Javadoc::class) {
    group = "manual"
    description = "Generate combined Javadoc for all published modules"

    destinationDir = file("$buildDir/api")

    val javadocTasks = rootProject.subprojects
        .filter { it.plugins.hasPlugin("java") && it.plugins.hasPlugin("build.publish") }
        .map { it.tasks.named<Javadoc>("javadoc").get() }

    javadocTasks.forEach { source(it.source) }
    classpath = files(javadocTasks.map { it.classpath })

    (options as StandardJavadocDocletOptions).run {
        isUse = true
        links("http://docs.oracle.com/javase/8/docs/api/")
        windowTitle = "MiniProfiler JVM API ($project.version)"
        docTitle =  "MiniProfiler JVM API ($project.version)"
    }
}

val packageManual by tasks.registering(Sync::class) {
    group = "manual"
    description = "Brings together manual and API reference"

    into("$buildDir/manual")
    from(tasks.asciidoctor)

    into("api") {
        from(api)
    }
}

val openApi by tasks.registering {
    dependsOn(packageManual)
    group = "manual"
    description = "Builds the API reference, then opens it in your web browser"

    doLast {
        getDesktop().browse(file("$packageManual.destinationDir/api/index.html").toURI())
    }
}

val openManual by tasks.registering {
    dependsOn(packageManual)
    group = "manual"
    description = "Builds the manual, then opens it in your web browser"

    doLast {
        getDesktop().browse(file("$packageManual.destinationDir/index.html").toURI())
    }
}

val manualZip by tasks.registering(Zip::class) {
    from(packageManual)
}

artifacts {
    archives(manualZip)
}

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "miniprofiler-manual"
        pom {
            name = "MiniProfiler Manual"
            description = "The manual for MiniProfiler JVM"
        }
    }
}
