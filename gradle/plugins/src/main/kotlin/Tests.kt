import buildparameters.BuildParametersExtension
import gradle.kotlin.dsl.accessors._082a9fc563e27823fee7f4ceb5b5d1a3.reporting
import gradle.kotlin.dsl.accessors._f6d9df91f901ae91cc345a25523b9e9b.testing
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.provider.Provider
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

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

fun JvmTestSuite.makeBrowserTest(project: Project, groovyVariant: String = "groovy4"): Unit {

    val reporting = project.extensions.getByType(ReportingExtension::class.java)
    val catalogs = project.extensions.getByType(VersionCatalogsExtension::class.java)
    val libs = catalogs.named("libs")

    dependencies {
        // type-safe project accessors are not available in precompiled script plugins
        implementation(project(":test-geb-$groovyVariant"))
        implementation(libs.findLibrary("geb-core-$groovyVariant").get())
        implementation(libs.findLibrary("geb-spock-$groovyVariant").get())
        implementation(libs.findLibrary("selenium-api-$groovyVariant").get())
        runtimeOnly(libs.findLibrary("selenium-firefox-driver-$groovyVariant").get())
        runtimeOnly(libs.findLibrary("selenium-support-$groovyVariant").get())
    }

    val buildParameters = project.extensions.getByType(BuildParametersExtension::class.java)
    val javaToolchains = project.extensions.getByType(JavaToolchainService::class.java)

    targets {
        all {
            testTask.configure {
                // Selenium 4.x requires Java 11+; use project's toolchain version if higher.
                // Resolved lazily so that toolchain overrides in the consumer script take effect.
//                javaLauncher.set(project.provider {
//                    val configuredVersion = project.extensions
//                        .findByType(JavaPluginExtension::class.java)
//                        ?.toolchain?.languageVersion?.orNull?.asInt() ?: 8
//                    javaToolchains.launcherFor {
//                        languageVersion.set(JavaLanguageVersion.of(maxOf(11, configuredVersion)))
//                    }.get()
//                })
                systemProperty("geb.build.reportsDir", "${reporting.baseDirectory.get().asFile}/geb")
                buildParameters.browserTest.firefoxBinPath.orNull?.let {
                    systemProperty("webdriver.firefox.bin", it)
                }
            }
        }
    }
}

fun Project.addTestSuite(suiteName: String, minJavaVersion: Int, configure: Action<JvmTestSuite> = Action {}): Provider<JvmTestSuite> {
    val capitalizedSuiteName = suiteName.replaceFirstChar { it.uppercase() }
    val javaPluginExt = extensions.findByType(JavaPluginExtension::class.java)!!
    val javaToolchains = extensions.getByType(JavaToolchainService::class.java)

    val maxVersion = javaPluginExt.toolchain.languageVersion.map {  maxOf(minJavaVersion, it.asInt()) }
    val launcher = javaToolchains.launcherFor {
        languageVersion = maxVersion.map(JavaLanguageVersion::of)
    }

    var suite: Provider<JvmTestSuite>? = null
    testing {
        suites {
            suite = register<JvmTestSuite>(suiteName) {
                targets.configureEach {
                    testTask.configure {
                        javaLauncher = launcher
                    }
                }
                configure.execute(this)
            }
        }
    }

    configurations {
        extendFromTest(suiteName)
    }

    tasks.withType<JavaCompile>().matching { it.name == "compile${capitalizedSuiteName}Java" }.configureEach {
        options.release = maxVersion
        javaCompiler = javaToolchains.compilerFor {
            languageVersion = maxVersion.map(JavaLanguageVersion::of)
        }
    }

    tasks.withType<GroovyCompile>().matching { it.name == "compile${capitalizedSuiteName}Groovy" }.configureEach {
        options.release = maxVersion
        javaLauncher = launcher
    }

    return suite!!
}

fun Project.addCrossVersionTestSuite(
    suiteName: String, minJavaVersion: Int, configure: Action<JvmTestSuite> = Action {}
): Provider<JvmTestSuite> {
    val sourceSets = extensions.getByType(SourceSetContainer::class.java)
    val suite = addTestSuite(suiteName, minJavaVersion) {
        dependencies {
            implementation(sourceSets.named("main").get().output)
        }
        configure.execute(this)
    }
    sourceSets.named(suiteName) {
        java.setSrcDirs(emptyList<String>())
        extensions.getByType(GroovySourceDirectorySet::class.java)
            .setSrcDirs(listOf("src/crossVersionTest/groovy"))
        resources.setSrcDirs(listOf("src/crossVersionTest/resources"))
    }
    tasks.named("fullCheck") { dependsOn(suite) }
    return suite
}

fun DependencyHandlerScope.scenarioTestFixtures(dependency: ProjectDependency): ProjectDependency {
    return dependency.also {
        it.capabilities {
            requireCapability("${it.group}:${it.name}-scenario-test-fixtures:${it.version}")
        }
    }
}
