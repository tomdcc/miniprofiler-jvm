import java.awt.Desktop.getDesktop
import org.gradle.jvm.toolchain.JavaToolchainService

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
    id("build.base")
    id("build.publish")
    id("java-base")
    alias(libs.plugins.asciidoctor)
}

repositories {
    mavenCentral()
}

tasks.asciidoctor.configure {
    baseDirFollowsSourceDir()
}

tasks.named("sanityCheck") {
    dependsOn(tasks.named("asciidoctor"))
    dependsOn(api)
}

val api by tasks.registering(Javadoc::class) {
    group = "manual"
    description = "Generate combined Javadoc for all published modules"

    destinationDir = layout.buildDirectory.dir("api").get().asFile

    javadocTool = project.extensions.getByType<JavaToolchainService>().javadocToolFor {
        languageVersion = JavaLanguageVersion.of(11)
    }

    val javadocTasks = rootProject.subprojects
        .filter { it.plugins.hasPlugin("java") && it.plugins.hasPlugin("build.publish") }
        .map { it.tasks.named<Javadoc>("javadoc").get() }

    javadocTasks.forEach { source(it.source) }
    classpath = files(javadocTasks.map { it.classpath })

    javadocTool = javaToolchains.javadocToolFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    (options as StandardJavadocDocletOptions).run {
        isUse = true
        links("https://docs.oracle.com/en/java/javase/25/docs/api/")
        windowTitle = "MiniProfiler JVM API ($project.version)"
        docTitle =  "MiniProfiler JVM API ($project.version)"
        memberLevel = JavadocMemberLevel.PROTECTED
        addBooleanOption("Xdoclint:all", true)
        addBooleanOption("Xdoclint/package:-io.jdev.miniprofiler.internal,-io.jdev.miniprofiler.ratpack.internal", true)
        addBooleanOption("Xwerror", true)
    }
}

val packageManual by tasks.registering(Sync::class) {
    group = "manual"
    description = "Brings together manual and API reference"

    into(layout.buildDirectory.dir("manual"))
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

publishing {
    publications.named<MavenPublication>("maven") {
        artifactId = "miniprofiler-manual"
        artifact(manualZip)
        pom {
            name = "MiniProfiler Manual"
            description = "The manual for MiniProfiler JVM"
        }
    }
}
