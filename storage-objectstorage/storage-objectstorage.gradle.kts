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
    id("build.container-test")
    id("build.publish")
    `java-test-fixtures`
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

    // testFixtures holds the base integration spec — it needs Spock/Groovy.
    testFixturesImplementation(libs.groovy.v4)
    testFixturesImplementation(libs.spock.groovy4)
}

dependencies {
    // compileOnly deps are not inherited by containerTest suite — re-declare them.
    containerTestImplementation(testFixtures(project))
    containerTestImplementation(libs.aws.s3)
    containerTestImplementation(libs.aws.auth)
    containerTestImplementation(libs.google.cloud.storage)
    containerTestImplementation(libs.google.auth.oauth2)
    containerTestImplementation(libs.azure.storage.blob)
    containerTestImplementation(libs.azure.identity)
}

tasks.named<Test>("containerTest").configure {
    systemProperty("dockerImage.s3mock", images.versions.s3mock.get())
    systemProperty("dockerImage.azurite", images.versions.azurite.get())
    systemProperty("dockerImage.fake-gcs-server", images.versions.fake.gcs.server.get())
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
