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

import org.gradle.api.Named
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

abstract class CrossVersionTestSuiteSpec @Inject constructor(private val _name: String) : Named {
    override fun getName(): String = _name
    abstract val minJavaVersion: Property<Int>

    internal val implementationDependencies = mutableListOf<Any>()
    internal val runtimeOnlyDependencies = mutableListOf<Any>()

    fun implementation(notation: Any) {
        addDep(notation, implementationDependencies)
    }

    fun runtimeOnly(notation: Any) {
        addDep(notation, runtimeOnlyDependencies)
    }

    // DependencyHandler.add(String, Object) does not support Provider<MinimalExternalModuleDependency>
    // or MinimalExternalModuleDependency directly. Resolve providers eagerly and convert catalog
    // entries to "group:name:version" coordinate strings which are universally supported.
    private fun addDep(notation: Any, target: MutableList<Any>) {
        if (notation is Provider<*>) {
            @Suppress("UNCHECKED_CAST")
            addDep((notation as Provider<Any>).get(), target)
            return
        }
        when (notation) {
            is MinimalExternalModuleDependency -> target.add(notation.toCoordinates())
            is ExternalModuleDependencyBundle -> notation.forEach { target.add(it.toCoordinates()) }
            else -> target.add(notation)
        }
    }

    private fun MinimalExternalModuleDependency.toCoordinates(): String {
        val version = versionConstraint.requiredVersion
        return if (version.isNotEmpty()) "${module.group}:${module.name}:$version"
        else "${module.group}:${module.name}"
    }
}
