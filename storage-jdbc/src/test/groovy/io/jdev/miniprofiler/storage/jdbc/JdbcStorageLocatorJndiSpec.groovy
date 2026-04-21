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

import com.zaxxer.hikari.HikariDataSource
import org.h2.jdbcx.JdbcDataSource
import spock.lang.Specification

import javax.naming.Context
import javax.naming.Name
import javax.naming.NameNotFoundException
import javax.naming.spi.InitialContextFactory
import javax.naming.spi.InitialContextFactoryBuilder
import javax.naming.spi.NamingManager

import java.util.concurrent.ConcurrentHashMap

class JdbcStorageLocatorJndiSpec extends Specification {

    private static final Map<String, Object> BINDINGS = new ConcurrentHashMap<>()

    static {
        try {
            NamingManager.setInitialContextFactoryBuilder({ env ->
                { e -> new MapBackedContext(BINDINGS) } as InitialContextFactory
            } as InitialContextFactoryBuilder)
        } catch (IllegalStateException ignored) {
            // Another test or runtime already installed a builder — acceptable
            // since this spec is the only test that uses JNDI in this module.
        }
    }

    def setup() {
        BINDINGS.clear()
    }

    def cleanup() {
        BINDINGS.clear()
        System.clearProperty("miniprofiler.storage.jdbc.jndiName")
        System.clearProperty("miniprofiler.storage.jdbc.url")
        System.clearProperty("miniprofiler.storage.jdbc.dialect")
    }

    def "locate uses a DataSource bound in JNDI and does not own it"() {
        given:
        def h2 = new JdbcDataSource()
        h2.URL = "jdbc:h2:mem:jnditest-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        BINDINGS["java:comp/env/jdbc/profiler"] = h2

        and:
        System.setProperty("miniprofiler.storage.jdbc.jndiName", "java:comp/env/jdbc/profiler")
        System.setProperty("miniprofiler.storage.jdbc.dialect", "h2")

        when:
        def result = new JdbcStorageLocator().locate()

        then:
        result.present
        def storage = (JdbcStorage) result.get()
        storage.dataSource.is(h2)

        when: "the storage is closed it must not close the container-managed DataSource"
        storage.close()
        def conn = h2.connection

        then:
        conn != null

        cleanup:
        conn?.close()
    }

    def "locate falls through from a missing JNDI binding to the URL path"() {
        given:
        System.setProperty("miniprofiler.storage.jdbc.jndiName", "java:comp/env/jdbc/missing")
        System.setProperty("miniprofiler.storage.jdbc.url", "jdbc:h2:mem:fallthrough-${UUID.randomUUID()};DB_CLOSE_DELAY=-1")

        when:
        def result = new JdbcStorageLocator().locate()

        then:
        result.present
        def storage = (JdbcStorage) result.get()
        storage.dataSource instanceof HikariDataSource

        cleanup:
        storage?.close()
    }

    def "locate returns empty when JNDI lookup fails and no URL is configured"() {
        given:
        System.setProperty("miniprofiler.storage.jdbc.jndiName", "java:comp/env/jdbc/missing")

        expect:
        !new JdbcStorageLocator().locate().present
    }

    def "locate returns empty when JNDI binding is not a DataSource and no URL is configured"() {
        given:
        BINDINGS["java:comp/env/jdbc/profiler"] = "not a data source"
        System.setProperty("miniprofiler.storage.jdbc.jndiName", "java:comp/env/jdbc/profiler")

        expect:
        !new JdbcStorageLocator().locate().present
    }

    private static class MapBackedContext implements Context {

        private final Map<String, Object> bindings

        MapBackedContext(Map<String, Object> bindings) {
            this.bindings = bindings
        }

        @Override
        Object lookup(String name) throws javax.naming.NamingException {
            Object value = bindings.get(name)
            if (value == null) {
                throw new NameNotFoundException(name)
            }
            return value
        }

        @Override Object lookup(Name name) throws javax.naming.NamingException { lookup(name.toString()) }
        @Override Hashtable<?, ?> getEnvironment() { new Hashtable<>() }
        @Override void close() {}
        @Override void bind(Name name, Object obj) { bindings.put(name.toString(), obj) }
        @Override void bind(String name, Object obj) { bindings.put(name, obj) }
        @Override void rebind(Name name, Object obj) { bindings.put(name.toString(), obj) }
        @Override void rebind(String name, Object obj) { bindings.put(name, obj) }
        @Override void unbind(Name name) { bindings.remove(name.toString()) }
        @Override void unbind(String name) { bindings.remove(name) }
        @Override void rename(Name a, Name b) { throw new UnsupportedOperationException() }
        @Override void rename(String a, String b) { throw new UnsupportedOperationException() }
        @Override javax.naming.NamingEnumeration<javax.naming.NameClassPair> list(Name n) { throw new UnsupportedOperationException() }
        @Override javax.naming.NamingEnumeration<javax.naming.NameClassPair> list(String n) { throw new UnsupportedOperationException() }
        @Override javax.naming.NamingEnumeration<javax.naming.Binding> listBindings(Name n) { throw new UnsupportedOperationException() }
        @Override javax.naming.NamingEnumeration<javax.naming.Binding> listBindings(String n) { throw new UnsupportedOperationException() }
        @Override void destroySubcontext(Name n) { throw new UnsupportedOperationException() }
        @Override void destroySubcontext(String n) { throw new UnsupportedOperationException() }
        @Override Context createSubcontext(Name n) { throw new UnsupportedOperationException() }
        @Override Context createSubcontext(String n) { throw new UnsupportedOperationException() }
        @Override Object lookupLink(Name n) { lookup(n) }
        @Override Object lookupLink(String n) { lookup(n) }
        @Override javax.naming.NameParser getNameParser(Name n) { throw new UnsupportedOperationException() }
        @Override javax.naming.NameParser getNameParser(String n) { throw new UnsupportedOperationException() }
        @Override Name composeName(Name n, Name p) { throw new UnsupportedOperationException() }
        @Override String composeName(String n, String p) { throw new UnsupportedOperationException() }
        @Override Object addToEnvironment(String k, Object v) { null }
        @Override Object removeFromEnvironment(String k) { null }
        @Override String getNameInNamespace() { "" }
    }
}
