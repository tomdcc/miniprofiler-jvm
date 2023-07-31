plugins {
    id("com.gradle.enterprise") version "3.11.1"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.11.1"
}

includeBuild("gradle/plugins")

include("miniprofiler-core")
include("miniprofiler-javaee")
include("miniprofiler-servlet")
include("miniprofiler-test-support")
include("miniprofiler-grails")
include("miniprofiler-ratpack")
include("miniprofiler-jooq")

include("doc:manual")

include("integration-test:servlet")
include("integration-test:glassfish4")
include("integration-test:wildfly8")
include("integration-test:ratpack")

rootProject.name = "miniprofiler-jvm"

fun setBuildFile(project: ProjectDescriptor) {
    project.buildFileName = if (File(project.projectDir, "${project.name}.gradle").exists()) "${project.name}.gradle" else "${project.name}.gradle.kts"
    project.children.forEach {
        setBuildFile(it)
    }
}

setBuildFile(rootProject)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val isCI = System.getenv("CI") != null
buildCache {
    local {
        isEnabled = !isCI
    }
}
