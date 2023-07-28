import com.bmuschko.gradle.cargo.convention.*

/*
 * Copyright 2018 the original author or authors.
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
    id("build.browser-test")
    id("build.cargo-test")
    id("build.java-module")
}

dependencies {
    implementation(project(":miniprofiler-javaee"))
    implementation(project(":miniprofiler-servlet"))

    testImplementation(project(":miniprofiler-test-support"))
}

val glassfishBaseDir = file("$buildDir/glassfish")
val glassfishDir by tasks.registering {
    doLast {
        glassfishBaseDir.mkdirs()
    }
}

listOf(tasks.cargoRunLocal, tasks.cargoStartLocal).forEach {
    it.configure {
        dependsOn(glassfishDir)
    }
}

// this sometimes hasn't cleaned up a process that uses the same port in time
tasks.cargoStartLocal.configure {
    shouldRunAfter(":integration-test:servlet:test")
}

cargo {
	containerId = "glassfish4x"
	port = 8081
	deployables = listOf(Deployable().apply {
		context = "/"
	})

	local(delegateClosureOf<CargoLocalTaskConvention> {
		installer(delegateClosureOf<ZipUrlInstaller> {
			installUrl = "https://download.java.net/glassfish/4.1.1/release/glassfish-4.1.1.zip"
			downloadDir = rootProject.file(".gradle/cache/cargo")
			extractDir = file("$buildDir/extract")
		})

		configHomeDir = glassfishBaseDir
		homeDir = glassfishBaseDir

		containerProperties(delegateClosureOf<ContainerProperties> {
			property("cargo.java.home", javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(8) }.get().metadata.installationPath.asFile.absolutePath)
			property("cargo.datasource.datasource1", "cargo.datasource.jndi=jdbc/DataSource|cargo.datasource.url=jdbc:h2:mem:miniprofiler;DB_CLOSE_DELAY=-1|cargo.datasource.driver=org.h2.Driver|cargo.datasource.username=sa|cargo.datasource.password=sa")
			if (project.hasProperty("debugCargo")) {
				property("cargo.glassfish.domain.debug", "true")
			}
		})

		file(delegateClosureOf<BinFile> {
			file = configurations.cargo.get().files.find { it.name.startsWith("h2-") }
			toDir = "cargo-domain/lib"
		})
	})
}
