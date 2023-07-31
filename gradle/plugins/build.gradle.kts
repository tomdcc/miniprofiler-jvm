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

plugins {
    // goes with this gradle version
    id("org.gradle.kotlin.kotlin-dsl") version "4.0.14"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // expose version catalog to these plugins
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(projects.buildParameters)

    // another workaround for https://github.com/gradle/gradle/issues/15383
    implementation(libs.cargo.plugin)
    implementation(libs.gradle.enterprise.plugin)
    implementation(libs.nexus.publish.plugin)
}
