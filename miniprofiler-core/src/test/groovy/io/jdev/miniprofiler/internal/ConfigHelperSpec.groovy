/*
 * Copyright 2016 the original author or authors.
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

package io.jdev.miniprofiler.internal

import spock.lang.Specification

class ConfigHelperSpec extends Specification {

    List<ConfigHelper.PropertiesWithPrefix> propsList = [
        new ConfigHelper.PropertiesWithPrefix(new Properties(), "prefix."),
        new ConfigHelper.PropertiesWithPrefix(new Properties(), "")
    ]

    enum TestEnum { FOO, BAR, BAZ }

    void 'can parse properties file in resource path'() {
        when:
        def props = ConfigHelper.loadPropertiesFile('test.properties')

        then:
        props.size() == 1
        props.foo == 'bar'
    }

    void 'does not choke on bad props file'() {
        when:
        def props = ConfigHelper.loadPropertiesFile('bad.properties')

        then:
        props == null
    }

    void 'does not choke on missing props file'() {
        when:
        def props = ConfigHelper.loadPropertiesFile('missing.properties')

        then:
        props == null
    }

    void 'returns first value found, or passed-in default if not found'() {
        given:
        propsList[0].props['prefix.a'] = '1'
        propsList[1].props['a'] = '2'
        propsList[1].props['b'] = '2'

        expect:
        ConfigHelper.getProperty(propsList, 'a', 'default') == '1'
        ConfigHelper.getProperty(propsList, 'b', 'default') == '2'
        ConfigHelper.getProperty(propsList, 'c', 'default') == 'default'
    }

    void 'handles ints correctly'() {
        given:
        propsList[0].props['prefix.a'] = '1'

        expect:
        ConfigHelper.getProperty(propsList, 'a', 99) == 1
        ConfigHelper.getProperty(propsList, 'b', 99) == 99
    }

    void 'handles booleans correctly'() {
        given:
        propsList[0].props['prefix.a'] = 'true'
        propsList[0].props['prefix.b'] = 'false'

        expect:
        ConfigHelper.getProperty(propsList, 'a', false) == true
        ConfigHelper.getProperty(propsList, 'b', true) == false
        ConfigHelper.getProperty(propsList, 'c', false) == false
        ConfigHelper.getProperty(propsList, 'c', true) == true
    }

    void 'handles enums correctly'() {
        given:
        propsList[0].props['prefix.a'] = 'foo'
        propsList[0].props['prefix.b'] = 'BAR'

        expect:
        ConfigHelper.getProperty(propsList, 'a', TestEnum, TestEnum.BAZ) == TestEnum.FOO
        ConfigHelper.getProperty(propsList, 'b', TestEnum, TestEnum.BAZ) == TestEnum.BAR
        ConfigHelper.getProperty(propsList, 'c', TestEnum, TestEnum.BAZ) == TestEnum.BAZ
    }

    void 'accepts null marker strings'() {
        given:
        propsList[0].props['prefix.a'] = ''
        propsList[1].props['a'] = '2'
        propsList[0].props['prefix.b'] = 'none'
        propsList[1].props['b'] = '2'
        propsList[0].props['prefix.c'] = 'NULL'

        expect:
        ConfigHelper.getProperty(propsList, 'a', 'default') == null
        ConfigHelper.getProperty(propsList, 'b', 'default') == null
        ConfigHelper.getProperty(propsList, 'c', 'default') == null
    }
}
