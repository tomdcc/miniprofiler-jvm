/*
 * Copyright 2013-2026 the original author or authors.
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
    id("build.build-scan")
    id("build.nexus-publish")
}

allprojects {
    group = "io.jdev.miniprofiler"
    version = "0.12.0"

    apply(plugin = "build.copyright")
}

val checkPublishedModulesMatchWorkflow by tasks.registering(CheckPublishedModulesTask::class) {
    group = "verification"
    description = "Verifies published modules and the release artifact validation workflow are in sync"
    workflowFile = file(".github/workflows/gradle-validate-release-artifacts.yml")
    publishedArtifactIds = provider {
        subprojects
            .filter { it.plugins.hasPlugin("build.publish") }
            .map { "miniprofiler-${it.name}" }
            .sorted()
    }
    resultFile = layout.buildDirectory.file("${name}/result.txt")
}

tasks.named("fullCheck") {
    dependsOn(checkPublishedModulesMatchWorkflow)
}
