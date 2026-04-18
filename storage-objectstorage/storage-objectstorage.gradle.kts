/*
 * Copyright 2026 the original author or authors.
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
    id("build.java-module")
    id("build.integration-test")
    id("build.publish")
}

dependencies {
    api(projects.core)

    // Cloud SDKs declared compileOnly — users bring their own versions.
    compileOnly(libs.aws.s3)
    compileOnly(libs.aws.auth)
    compileOnly(libs.google.cloud.storage)
    compileOnly(libs.google.auth.oauth2)
    compileOnly(libs.azure.storage.blob)
    compileOnly(libs.azure.identity)

    // compileOnly deps are not inherited by integrationTest suite — re-declare them.
    integrationTestImplementation(libs.aws.s3)
    integrationTestImplementation(libs.aws.auth)
    integrationTestImplementation(libs.google.cloud.storage)
    integrationTestImplementation(libs.google.auth.oauth2)
    integrationTestImplementation(libs.azure.storage.blob)
    integrationTestImplementation(libs.azure.identity)
    integrationTestImplementation(libs.testcontainers.core)
    integrationTestImplementation(libs.testcontainers.junit5)
}

tasks.named<Test>("integrationTest") {
    environment("DOCKER_API_VERSION", "1.45")
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Object Storage"
            description = "AWS S3, Google Cloud Storage and Azure Blob Storage backends for MiniProfiler JVM."
        }
    }
}
