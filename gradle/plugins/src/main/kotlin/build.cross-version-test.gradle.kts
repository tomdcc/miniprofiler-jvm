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

plugins {
    id("build.java-module")
}

val crossVersionTests = project.objects.domainObjectContainer(CrossVersionTestSuiteSpec::class.java)
extensions.add("crossVersionTests", crossVersionTests)

val crossVersionTest = tasks.register("crossVersionTest") {
    group = "verification"
    description = "Runs all cross-version test suites."
}

tasks.named("fullCheck") { dependsOn(crossVersionTest) }

// all{} fires on a freshly-created spec before register{} and configureEach{} configure actions
// have run (Gradle fires whenObjectAdded at creation time). Defer all spec-reading to afterEvaluate,
// by which point the spec's dep lists and minJavaVersion have been fully populated.
crossVersionTests.all {
    val spec = this
    project.afterEvaluate {
        val suite = addTestSuite(spec.name, spec.minJavaVersion.getOrElse(8)) { }

        // Set source dirs to the shared crossVersionTest source tree
        sourceSets.named(spec.name) {
            java.setSrcDirs(emptyList<String>())
            extensions.getByType(org.gradle.api.tasks.GroovySourceDirectorySet::class.java)
                .setSrcDirs(listOf("src/crossVersionTest/groovy"))
            resources.setSrcDirs(listOf("src/crossVersionTest/resources"))
        }

        // Cut all ties to testImplementation/testRuntimeOnly
        configurations.named("${spec.name}Implementation") { setExtendsFrom(emptyList()) }
        configurations.named("${spec.name}RuntimeOnly") { setExtendsFrom(emptyList()) }

        // Apply deps from the spec. Catalog entries have been eagerly converted to coordinate
        // strings in CrossVersionTestSuiteSpec.addDep(), so DependencyHandler.add(String, Object)
        // handles them correctly.
        dependencies {
            "${spec.name}Implementation"(sourceSets.named("main").get().output)
            spec.implementationDependencies.forEach { dep -> "${spec.name}Implementation"(dep) }
            spec.runtimeOnlyDependencies.forEach { dep -> "${spec.name}RuntimeOnly"(dep) }
        }

        crossVersionTest { dependsOn(suite) }
    }
}
