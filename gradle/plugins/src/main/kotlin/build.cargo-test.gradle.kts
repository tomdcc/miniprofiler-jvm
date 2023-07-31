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
    id("com.bmuschko.cargo")
    id("java-library")
    id("war")
}

val libs = the<LibrariesForLibs>()

dependencies {
    compileOnly(libs.javaee)
    cargo(libs.cargo.ant)
    cargo(libs.cargo.core)
    cargo(libs.h2)
}

val cargoRunLocal = tasks.named("cargoRunLocal")
val cargoStartLocal = tasks.named("cargoStartLocal")
val cargoStopLocal = tasks.named("cargoStopLocal")

listOf(cargoRunLocal, cargoStartLocal).forEach {
    it.configure {
        dependsOn(":miniprofiler-core:assemble")
        dependsOn(tasks.named("assemble"))
    }
}

tasks.withType<Test> {
    dependsOn(cargoStartLocal)
}

cargoStartLocal.configure {
    finalizedBy(cargoStopLocal)
}

cargoStopLocal.configure {
    mustRunAfter(tasks.withType<Test>())
}
