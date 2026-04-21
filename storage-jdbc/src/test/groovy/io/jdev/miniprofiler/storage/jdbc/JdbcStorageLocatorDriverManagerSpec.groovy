/*
 * Copyright 2026 the original author or authors.
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

package io.jdev.miniprofiler.storage.jdbc

import spock.lang.Specification

/**
 * Exercises the Hikari-absent path of {@link JdbcStorageLocator} without physically removing
 * Hikari from the test classpath. A filtering classloader redefines the storage-jdbc classes
 * so their {@code Class.forName("com.zaxxer.hikari...")} probes resolve against the filtered
 * loader and report Hikari as missing.
 */
class JdbcStorageLocatorDriverManagerSpec extends Specification {

    static class HikariHidingClassLoader extends ClassLoader {
        HikariHidingClassLoader(ClassLoader parent) {
            super(parent)
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith('com.zaxxer.hikari.')) {
                throw new ClassNotFoundException(name)
            }
            // Re-define storage-jdbc classes in THIS classloader so Class.forName
            // inside them uses the filtered loader. Delegate everything else.
            if (name.startsWith('io.jdev.miniprofiler.storage.jdbc.')) {
                Class<?> c = findLoadedClass(name)
                if (c == null) {
                    def resource = getParent().getResource(name.replace('.', '/') + '.class')
                    if (resource == null) {
                        throw new ClassNotFoundException(name)
                    }
                    byte[] bytes = resource.bytes
                    c = defineClass(name, bytes, 0, bytes.length)
                }
                if (resolve) {
                    resolveClass(c)
                }
                return c
            }
            return super.loadClass(name, resolve)
        }
    }

    def "locate falls back to DriverManagerDataSource when Hikari is absent"() {
        given:
        def url = "jdbc:h2:mem:dmtest-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        System.setProperty("miniprofiler.storage.jdbc.url", url)

        and:
        def loader = new HikariHidingClassLoader(getClass().getClassLoader())
        def locatorClass = loader.loadClass("io.jdev.miniprofiler.storage.jdbc.JdbcStorageLocator")
        def locator = locatorClass.getDeclaredConstructor().newInstance()

        when:
        def result = locator.locate()

        then:
        result.present

        when:
        def storage = result.get()
        def dataSource = storage.dataSource

        then:
        // Classes differ across classloaders — compare names as strings.
        dataSource.getClass().getName() == "io.jdev.miniprofiler.storage.jdbc.DriverManagerDataSource"

        when: "the fallback yields a usable storage"
        storage.createTable()
        def conn = dataSource.getConnection()

        then:
        conn != null

        cleanup:
        conn?.close()
        storage?.close()
        System.clearProperty("miniprofiler.storage.jdbc.url")
    }
}
