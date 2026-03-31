import buildparameters.BuildParametersExtension
import gradle.kotlin.dsl.accessors._082a9fc563e27823fee7f4ceb5b5d1a3.reporting
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.invoke

// Inherit unit test dependencies (Spock, Groovy, JUnit Platform, etc.) from the build.java-module
// convention plugin so they don't need to be redeclared for integration tests.
fun ConfigurationContainer.extendFromTest(testSuiteName: String) {
    named("${testSuiteName}Implementation") {
        extendsFrom(named("testImplementation").get())
    }
    named("${testSuiteName}RuntimeOnly") {
        extendsFrom(named("testRuntimeOnly").get())
    }
}

fun JvmTestSuite.makeBrowserTest(project: Project): Unit {

    val catalogs = project.extensions.getByType(VersionCatalogsExtension::class.java)
    val libs = catalogs.named("libs")

    dependencies {
        implementation(project())
        // type-safe project accessors are not available in precompiled script plugins
        implementation(project(":miniprofiler-test-geb"))
        implementation(libs.findLibrary("geb-core").get())
        implementation(libs.findLibrary("geb-spock").get())
        implementation(libs.findLibrary("selenium-api").get())
        runtimeOnly(libs.findLibrary("selenium-firefox-driver").get())
        runtimeOnly(libs.findLibrary("selenium-support").get())
    }

    val buildParameters = project.extensions.getByType(BuildParametersExtension::class.java)

    targets {
        all {
            testTask.configure {
                systemProperty("geb.build.reportsDir", "${project.reporting.baseDirectory.get().asFile}/geb")
                buildParameters.browserTest.firefoxBinPath.orNull?.let {
                    systemProperty("webdriver.firefox.bin", it)
                }
            }
        }
    }
}
