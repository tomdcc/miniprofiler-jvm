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

// This is necessary as Gradle had issues resolving a configuration
// that included both our servlet modules and their test fixtures, as
// our normal consumption of those was jars (for the tld to get picked
// up), but the Gradle built-in java-test-fixtures plugin automatically
// adds the main project to the test fixtures compile classpath.

plugins {
    id("build.build-parameters")
    id("java-library")
}

val sourceSets = extensions.getByType<SourceSetContainer>()

val scenarioTestFixtures = sourceSets.create("scenarioTestFixtures")

val fixturesJar = tasks.register<Jar>("scenarioTestFixturesJar") {
    from(scenarioTestFixtures.output)
    archiveClassifier.set("scenario-test-fixtures")
}

val capability = "${project.group}:${project.name}-scenario-test-fixtures:${project.version}"

configurations.create("scenarioTestFixturesApiElements") {
    isCanBeResolved = false
    isCanBeConsumed = true
    outgoing.capability(capability)
    outgoing.artifact(fixturesJar)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(Usage.JAVA_API))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named<LibraryElements>(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named<Bundling>(Bundling.EXTERNAL))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, JavaVersion.VERSION_1_8.majorVersion.toInt())
    }
}

configurations.create("scenarioTestFixturesRuntimeElements") {
    isCanBeResolved = false
    isCanBeConsumed = true
    extendsFrom(configurations["scenarioTestFixturesImplementation"])
    extendsFrom(configurations["scenarioTestFixturesRuntimeOnly"])
    outgoing.capability(capability)
    outgoing.artifact(fixturesJar)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named<LibraryElements>(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named<Bundling>(Bundling.EXTERNAL))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, JavaVersion.VERSION_1_8.majorVersion.toInt())
    }
}
