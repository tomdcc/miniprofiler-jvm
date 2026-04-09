/*
 * Copyright 2026 the original author or authors.
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
    id("build.java-module")
    id("build.publish")
}

dependencies {
    api(projects.core)
    compileOnly(libs.hibernate.v5) {
        isTransitive = false
    }
    testImplementation(projects.test)
    testImplementation(libs.hibernate.v5)
}

val crossVersionTestHibernate5 = addCrossVersionTestSuite("crossVersionTestHibernate5", 8) {
    dependencies {
        implementation(libs.hibernate.v5)
        implementation(libs.h2)
    }
}
addCrossVersionTestSuite("crossVersionTestHibernate6", 11) {
    dependencies {
        implementation(libs.hibernate.v6)
        implementation(libs.h2)
    }
}
addCrossVersionTestSuite("crossVersionTestHibernate7", 17) {
    dependencies {
        implementation(libs.hibernate.v7)
        implementation(libs.h2)
    }
}

tasks.named("check") { dependsOn(crossVersionTestHibernate5) }

configurations {
    // Exclude the compile-time Hibernate (org.hibernate:hibernate-core) from suites that use
    // Hibernate 6+, which changed groupId to org.hibernate.orm
    "crossVersionTestHibernate6Implementation" {
        exclude(group = "org.hibernate", module = "hibernate-core")
    }
    "crossVersionTestHibernate7Implementation" {
        exclude(group = "org.hibernate", module = "hibernate-core")
    }
}

publishing {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name = "MiniProfiler Hibernate Module"
            description = "Hibernate SQL profiling support for MiniProfiler."
        }
    }
}
