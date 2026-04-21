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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CheckPublishedModulesTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val workflowFile: RegularFileProperty

    @get:Input
    abstract val publishedArtifactIds: ListProperty<String>

    @get:OutputFile
    abstract val resultFile: RegularFileProperty

    @TaskAction
    fun check() {
        val workflowContent = workflowFile.asFile.get().readText()
        val published = publishedArtifactIds.get().toSet()
        val workflowArtifacts = Regex("""^\s+(\S+)\s*$""", RegexOption.MULTILINE)
            .findAll(
                workflowContent.substringAfter("ARTIFACTS=(").substringBefore(")")
            )
            .map { it.groupValues[1] }
            .toSet()

        val notInWorkflow = published - workflowArtifacts
        val notPublished = workflowArtifacts - published

        val errors = buildList {
            if (notInWorkflow.isNotEmpty()) {
                add("Published modules not listed in ${workflowFile.asFile.get().name}: $notInWorkflow")
            }
            if (notPublished.isNotEmpty()) {
                add("Modules listed in ${workflowFile.asFile.get().name} but not published: $notPublished")
            }
        }
        if (errors.isNotEmpty()) {
            throw GradleException(errors.joinToString("\n"))
        }

        resultFile.asFile.get().apply {
            parentFile.mkdirs()
            writeText("OK\n")
        }
    }
}
