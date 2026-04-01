/*
 * Copyright 2013 the original author or authors.
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

package io.jdev.miniprofiler.storage

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.ProfilerProvider
import net.sf.ehcache.Ehcache
import net.sf.ehcache.Element
import spock.lang.Specification

class EhcacheStorageSpec extends Specification {

    EhcacheStorage storage
    ProfilerProvider profilerProvider
    Ehcache cache

    void setup() {
        cache = Mock(Ehcache)
        storage = new EhcacheStorage(cache)
        profilerProvider = Mock(ProfilerProvider)
    }

    void "ehcache storage stores saved value"() {
        given:
        def val = new ProfilerImpl('test', ProfileLevel.Info, profilerProvider)

        when:
        storage.save(val)

        then: 'stored in underlying cache'
        1 * cache.put(_ as Element) >> { Element element ->
            assert element.objectKey == val.id
            assert element.objectValue == val
        }
    }

    void "ehcache storage returns value returned by cache"() {
        given: 'value stored in cache'
        def val = new ProfilerImpl('test1', ProfileLevel.Info, profilerProvider)
        def element = new Element(val.id, val)

        when: 'ask for val'
        def returned = storage.load(val.id)

        then: ' fetched from cache and returned'
        1 * cache.get(val.id) >> element
        returned == val
    }

    void "ehcache storage returns null when null returned by cache"() {
        given: 'id not in cache'
        def id = UUID.randomUUID()

        when: 'ask for val'
        def returned = storage.load(id)

        then: 'cache queried'
        1 * cache.get(id) >> null

        and: 'null returned'
        returned == null
    }

    void "ehcache storage returns null when element returned by cache has expired"() {
        given: 'expiredvalue stored in cache'
        def val = new ProfilerImpl('test1', ProfileLevel.Info, profilerProvider)
        def element = new Element(val.id, val, 1, 1)
        Thread.sleep(element.expirationTime - System.currentTimeMillis() + 10)
        assert element.expired

        when: 'ask for val'
        def returned = storage.load(val.id)

        then: 'cache queried'
        1 * cache.get(val.id) >> element

        and: 'null returned'
        returned == null
    }

}
