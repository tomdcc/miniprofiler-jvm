import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.plugins.JavaPluginExtension
import java.net.URI

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
    repositories {
        maven {
            name = "sonatypeStaging"
            url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials(PasswordCredentials::class.java)
        }
        maven {
            name = "sonatypeSnapshots"
            url = URI("https://oss.sonatype.org/content/repositories/snapshots/")
            credentials(PasswordCredentials::class.java)
        }
    }

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

signing {
    sign(publishing.publications["maven"])
    setRequired(project.provider { project.hasProperty("forceSigning") || !(project.ext.get("isSnapshot") as Boolean) && gradle.taskGraph.hasTask("${project.path}:publishMavenPublicationToSonatypeStagingRepository") })
}

// disable metadata publication, messes with using the shadow jar
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.register("publishSnapshots") {
    if ((project.version as String).endsWith("-SNAPSHOT")) {
        dependsOn(tasks.named("publishMavenPublicationToSonatypeSnapshotsRepository"))
    }
}
