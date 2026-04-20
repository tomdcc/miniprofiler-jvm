import buildparameters.BuildParametersExtension

pluginManagement {
    includeBuild("gradle/plugins")
}

plugins {
    id("build.build-parameters")
    id("com.gradle.develocity") version "4.4.1"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.6.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("gradle/plugins")

include("core")
include("eclipselink")
include("hibernate")
include("jakarta-ee")
include("jakarta-servlet")
include("javax-ee")
include("javax-servlet")
include("jdbc")
include("jooq")
include("ratpack")
include("storage-jdbc")
include("storage-objectstorage")
include("test")
include("test-geb-groovy3")
include("test-geb-groovy4")
include("viewer")

include("doc:manual")

include("testlib-browser")
include("testlib-integration")

include("scenario-test:glassfish4")
include("scenario-test:glassfish7")
include("scenario-test:jetty12-servlet")
include("scenario-test:jetty9-servlet")
include("scenario-test:ratpack1")
include("scenario-test:wildfly10")
include("scenario-test:wildfly27")

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
        isEnabled = true
        isPush = !buildParameters.ci || buildParameters.ciBranch == "main"
    }
}
