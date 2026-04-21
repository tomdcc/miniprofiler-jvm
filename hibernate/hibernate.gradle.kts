/*
 * Copyright 2016-2026 the original author or authors.
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
    id("build.cross-version-test")
    id("build.java-module")
    id("build.publish")
}

java {
    // Hibernate 7.x class files require a Java 17 compiler to read
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Compiled against Hibernate 7.2+, which still exposes the now-deprecated DatasourceConnectionProviderImpl
// as a shim alongside the renamed DataSourceConnectionProvider, letting LegacyProfilingDatasourceConnectionProvider
// and ProfilingDatasourceConnectionProvider coexist in the same compilation unit.
tasks.withType<JavaCompile>().matching { it.name == "compileJava" }.configureEach {
    // Output Java 8 bytecode so the module can deploy to Java 8 containers (e.g. WildFly 10).
    options.release = 8
}

dependencies {
    api(projects.core)
    api(projects.jdbc)
    compileOnly(libs.hibernate.v7) {
        isTransitive = false
    }

    testImplementation(projects.test)
    testImplementation(libs.hibernate.v7)
}

crossVersionTests {
    configureEach {
        implementation(libs.bundles.testing.groovy4)
        runtimeOnly(libs.bundles.testing.runtime)
        implementation(projects.test)
        implementation(projects.jdbc)
        implementation(libs.h2)
    }
    register("crossVersionTestHibernate5") {
        minJavaVersion = 8
        implementation(libs.hibernate.v5)
    }
    register("crossVersionTestHibernate6") {
        minJavaVersion = 11
        implementation(libs.hibernate.v6)
    }
    register("crossVersionTestHibernate70") {
        minJavaVersion = 17
        implementation(libs.hibernate.v70)
    }
    register("crossVersionTestHibernate7") {
        minJavaVersion = 17
        implementation(libs.hibernate.v7)
    }
}

tasks.named("check") { dependsOn("crossVersionTestHibernate5") }

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Hibernate Module"
            description = "Hibernate SQL profiling support for MiniProfiler."
        }
    }
}
