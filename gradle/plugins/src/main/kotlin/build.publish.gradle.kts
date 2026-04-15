/*
 * Copyright 2023-2026 the original author or authors.
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
    id("base")
    id("build.build-parameters")
    id("maven-publish")
    id("signing")
}

project.plugins.withId("java") {
    project.extensions.configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
    }
}

project.plugins.withId("java-test-fixtures") {
    val javaComponent = project.components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(project.configurations["testFixturesApiElements"]) { skip() }
    javaComponent.withVariantsFromConfiguration(project.configurations["testFixturesRuntimeElements"]) { skip() }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "miniprofiler-${project.name}"
            pom {
                url = "https://github.com/tomdcc/miniprofiler-jvm/"
                inceptionYear = "2013"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "tomdcc"
                        name = "Tom Dunstan"
                    }
                }
                scm {
                    url = "https://github.com/tomdcc/miniprofiler-jvm/"
                }
            }
        }
    }
}

val isSnapshot = (project.version as String).endsWith("-SNAPSHOT")

signing {
    val signingKey: String? = findProperty("signingKey") as String?
    val signingPassword: String? = findProperty("signingPassword") as String?
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications["maven"])
    setRequired(project.provider {
        val publishingToStaging = !isSnapshot && gradle.taskGraph.hasTask("${project.path}:publishToSonatype")
        buildParameters.publishing.alwaysSign || publishingToStaging
    })
}

// disable metadata publication, messes with using the shadow jar
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

// bundle the project LICENSE into all published code jars (skip sources/javadoc jars)
tasks.withType<Jar>().configureEach {
    if (name != "sourcesJar" && name != "javadocJar") {
        from(rootProject.file("LICENSE.txt")) {
            into("META-INF")
            rename { "LICENSE" }
        }
    }
}

tasks.register("publishSnapshots") {
    if (isSnapshot) {
        dependsOn(tasks.named("publishToSonatype"))
    }
}
