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

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.the

plugins {
    id("build.build-parameters")
    id("java-library")
}

val libs = the<LibrariesForLibs>()

val suiteName = "integrationTest"
testing {
    suites {
        register<JvmTestSuite>(suiteName) {
            makeBrowserTest(project)
        }
    }
}

configurations {
    extendFromTest(suiteName)
}

// Force integrationTest compilation to Java 11+ (Selenium 4.x requires it), while allowing
// modules that already target a higher version (e.g. Java 17) to keep their configured version.
// Capture project-scope objects here; inside configureEach{} the receiver changes to the task.
val javaPluginExt = extensions.findByType(JavaPluginExtension::class.java)
val javaToolchains = extensions.getByType(JavaToolchainService::class.java)
tasks.withType<JavaCompile>().matching { it.name.startsWith("compileIntegrationTest") }.configureEach {
    javaCompiler.set(project.provider {
        val configuredVersion = javaPluginExt?.toolchain?.languageVersion?.orNull?.asInt() ?: 8
        javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(maxOf(11, configuredVersion)))
        }.get()
    })
}
tasks.withType<GroovyCompile>().matching { it.name.startsWith("compileIntegrationTest") }.configureEach {
    javaLauncher.set(project.provider {
        val configuredVersion = javaPluginExt?.toolchain?.languageVersion?.orNull?.asInt() ?: 8
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(maxOf(11, configuredVersion)))
        }.get()
    })
}

tasks.named("check") {
    dependsOn(testing.suites.named(suiteName))
}
