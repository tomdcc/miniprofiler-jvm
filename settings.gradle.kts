import buildparameters.BuildParametersExtension

pluginManagement {
    includeBuild("gradle/plugins")
}

plugins {
    id("build.build-parameters")
    id("com.gradle.develocity") version "4.4.0"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.4.0"
}

includeBuild("gradle/plugins")

include("miniprofiler-core")
include("miniprofiler-eclipselink")
include("miniprofiler-hibernate")
include("miniprofiler-jakarta-ee")
include("miniprofiler-jakarta-servlet")
include("miniprofiler-javax-ee")
include("miniprofiler-javax-servlet")
include("miniprofiler-jooq")
include("miniprofiler-ratpack")
include("miniprofiler-test")
include("miniprofiler-test-geb-groovy3")
include("miniprofiler-test-geb-groovy4")
include("miniprofiler-viewer")

include("doc:manual")

include("integration-test:glassfish4")
include("integration-test:glassfish7")
include("integration-test:lib")
include("integration-test:jakarta-servlet")
include("integration-test:javax-servlet")
include("integration-test:ratpack")
include("integration-test:wildfly8")
include("integration-test:wildfly27")

rootProject.name = "miniprofiler-jvm"

fun setBuildFile(project: ProjectDescriptor) {
    project.buildFileName = if (File(project.projectDir, "${project.name}.gradle").exists()) "${project.name}.gradle" else "${project.name}.gradle.kts"
    project.children.forEach {
        setBuildFile(it)
    }
}

setBuildFile(rootProject)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val buildParameters = the<BuildParametersExtension>()
buildCache {
    local {
        isEnabled = !buildParameters.ci
    }
}
