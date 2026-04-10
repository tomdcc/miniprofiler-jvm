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

import org.gradle.process.CommandLineArgumentProvider

// Guice 4.x (a transitive dependency of ratpack) uses ClassLoader.defineClass via reflection,
// which is illegal on Java 9+ without an explicit open. The argument is a no-op on Java 8.
project.tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        val version = javaLauncher.orNull?.metadata?.languageVersion?.asInt()
            ?: JavaVersion.current().majorVersion.toInt()
        if (version >= 9) listOf("--add-opens=java.base/java.lang=ALL-UNNAMED") else emptyList()
    })
}
