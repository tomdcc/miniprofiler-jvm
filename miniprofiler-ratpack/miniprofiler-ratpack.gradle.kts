/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("build.java-module")
    id("build.publish")
    id("io.ratpack.ratpack-base")
}

dependencies {
    api(projects.miniprofilerCore)
    compileOnly(ratpack.dependency("core"))
    compileOnly(ratpack.dependency("guice"))
    compileOnly(ratpack.dependency("hikari"))
    compileOnly(ratpack.dependency("h2"))

    testImplementation(ratpack.dependency("test"))
    testImplementation(ratpack.dependency("groovy-test"))
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Ratpack Support"
            description = "Support classes for getting the MiniProfiler working out of the box in Ratpack."
        }
    }
}
