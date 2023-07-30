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
    id("base")
    id("maven-publish")
    id("signing")
}

project.plugins.withId("java") {
    project.extensions.configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
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
    sign(publishing.publications["maven"])
    setRequired(project.provider {
        val publishingToStaging = !isSnapshot && gradle.taskGraph.hasTask("${project.path}:publishToSonatype")
        project.hasProperty("forceSigning") || rootProject.extra["isCI"] as Boolean || publishingToStaging
    })
}

// disable metadata publication, messes with using the shadow jar
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.register("publishSnapshots") {
    if (isSnapshot) {
        dependsOn(tasks.named("publishToSonatype"))
    }
}
