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
    implementation(projects.miniprofilerJavaee)
    implementation(projects.miniprofilerServlet)
}

val wildflyBaseDir = file("$buildDir/wildfly")
val wildflyDir by tasks.registering {
    doLast {
	    wildflyBaseDir.mkdirs()
    }
}

listOf(tasks.cargoRunLocal, tasks.cargoStartLocal).forEach {
    it.configure {
        dependsOn(wildflyDir)
    }
}

cargo {
	containerId = "wildfly8x"
	port = 8081
    deployables = listOf(Deployable().apply {
        context = "/"
    })

    local(delegateClosureOf<CargoLocalTaskConvention> {
        installer(delegateClosureOf<ZipUrlInstaller> {
			installUrl = "https://download.jboss.org/wildfly/8.1.0.Final/wildfly-8.1.0.Final.tar.gz"
			downloadDir = rootProject.file(".gradle/cache/cargo")
			extractDir = file("$buildDir/extract")
		})

		configHomeDir = wildflyBaseDir
		homeDir = wildflyBaseDir

        containerProperties(delegateClosureOf<ContainerProperties> {
            property("cargo.java.home", javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(8) }.get().metadata.installationPath.asFile.absolutePath)
        })

        file(delegateClosureOf<BinFile> {
			file = configurations.cargo.get().files.find { it.name.startsWith("h2-") }
			toDir = "lib"
		})
	})
}
