/*
 * Copyright 2023 the original author or authors.
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
import org.gradle.kotlin.dsl.the

plugins {
    id("checkstyle")
}

val libs = the<LibrariesForLibs>()

dependencies {
    checkstyle(libs.checkstyle)
}

var checkstyleConfigDir = rootProject.file("gradle/checkstyle")

checkstyle {
    configDirectory = checkstyleConfigDir
    configProperties["checkstyleConfigDir"] = checkstyleConfigDir
}

plugins.withType<GroovyBasePlugin> {
    project.the<JavaPluginExtension>().sourceSets.configureEach {
        tasks.register<Checkstyle>(getTaskName("checkstyle", "groovy")) {
            configFile = File(checkstyleConfigDir, "checkstyle-groovy.xml")
            source(this@configureEach.the<GroovySourceDirectorySet>())
            classpath = compileClasspath
            reports.xml.outputLocation = File(checkstyle.reportsDir, "${name}-groovy.xml")
        }
    }
}

val checkstyleTasks = tasks.withType<Checkstyle>()

tasks.register("checkstyle") {
    dependsOn(checkstyleTasks)
}

plugins.withType<JavaBasePlugin> {
    tasks.named(JavaBasePlugin.CHECK_TASK_NAME).configure {
        dependsOn(checkstyleTasks)
    }
}
