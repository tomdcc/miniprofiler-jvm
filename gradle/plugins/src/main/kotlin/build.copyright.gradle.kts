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

import java.io.File

plugins {
    id("build.base")
}

// Every project (root + subprojects) gets its own pair of tasks scoped to its own directory.
// For any project, we exclude the project directories of every descendant project, so that files
// in nested subprojects are handled exclusively by those subprojects' tasks — no double-counting,
// and running e.g. `:verifyCopyright` on the root aggregates naturally via Gradle's cross-project
// task invocation.
val thisProject = project
val thisProjectDir: File = project.projectDir

val descendantProjectDirs = provider {
    val thisPath = thisProjectDir.toPath().toAbsolutePath().normalize()
    rootProject.allprojects
        .asSequence()
        .filter { it !== thisProject }
        .map { it.projectDir }
        .filter { dir ->
            val otherPath = dir.toPath().toAbsolutePath().normalize()
            otherPath.startsWith(thisPath) && otherPath != thisPath
        }
        .toList()
}

tasks.register<UpdateCopyrightTask>("updateCopyright") {
    group = "documentation"
    description = "Updates file-header copyright years from git history for this project's directory."
    repositoryRoot.set(rootProject.layout.projectDirectory)
    scanDirectory.set(layout.projectDirectory)
    excludedDirectories.set(descendantProjectDirs)
    notCompatibleWithConfigurationCache("Shells out to git during execution.")
}

val verifyCopyright = tasks.register<VerifyCopyrightTask>("verifyCopyright") {
    group = "verification"
    description = "Fails if any file under this project has an out-of-date copyright year."
    repositoryRoot.set(rootProject.layout.projectDirectory)
    scanDirectory.set(layout.projectDirectory)
    excludedDirectories.set(descendantProjectDirs)
    notCompatibleWithConfigurationCache("Shells out to git during execution.")

    val repoRoot = rootProject.projectDir
    val scanDir = layout.projectDirectory.asFile
    val excluded = descendantProjectDirs

    // Pathspecs that exclude descendant project directories, e.g. ":(exclude)core/".
    // Used in both git log and git status to mirror the task's own exclusion logic.
    fun excludePathspecs(): List<String> =
        excluded.get().map { ":(exclude)${it.path}" }

    lastCommitId.set(provider {
        CopyrightGit.run(repoRoot,
            listOf("log", "-1", "--format=%H", "--", scanDir.path) + excludePathspecs()
        ).trim()
    })
    markerFile.set(layout.buildDirectory.file("verifyCopyright/upToDate.marker"))
    outputs.upToDateWhen {
        CopyrightGit.run(repoRoot,
            listOf("status", "--porcelain", "--", scanDir.path) + excludePathspecs()
        ).isBlank()
    }
}

tasks.register<Delete>("cleanVerifyCopyright") {
    group = "verification"
    description = "Removes the verifyCopyright up-to-date marker file."
    delete(verifyCopyright.flatMap { it.markerFile })
}

tasks.named("fullCheck") {
    dependsOn(verifyCopyright)
}
