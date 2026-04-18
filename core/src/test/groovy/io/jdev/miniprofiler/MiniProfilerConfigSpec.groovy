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

package io.jdev.miniprofiler

import spock.lang.Specification

class MiniProfilerConfigSpec extends Specification {

    enum TestEnum { FOO, BAR, BAZ }

    void 'can parse properties file in resource path'() {
        when:
        def props = MiniProfilerConfig.loadPropertiesFile('test.properties')

        then:
        props.size() == 1
        props.foo == 'bar'
    }

    void 'does not choke on bad props file'() {
        when:
        def props = MiniProfilerConfig.loadPropertiesFile('bad.properties')

        then:
        props == null
    }

    void 'does not choke on missing props file'() {
        when:
        def props = MiniProfilerConfig.loadPropertiesFile('missing.properties')

        then:
        props == null
    }

    void 'returns system property value with miniprofiler. prefix'() {
        given:
        def sysProps = ['miniprofiler.a': '1'] as Properties
        def fileProps = ['a': '2', 'b': '2'] as Properties
        def config = new MiniProfilerConfig(sysProps, fileProps)

        expect:
        config.getProperty('a', 'default') == '1'
        config.getProperty('b', 'default') == '2'
        config.getProperty('c', 'default') == 'default'
    }

    void 'handles ints correctly'() {
        given:
        def sysProps = ['miniprofiler.a': '1'] as Properties
        def config = new MiniProfilerConfig(sysProps, null)

        expect:
        config.getProperty('a', 99) == 1
        config.getProperty('b', 99) == 99
    }

    void 'handles Integer correctly, returning null for null-marker values'() {
        given:
        def sysProps = ['miniprofiler.a': '1', 'miniprofiler.b': ''] as Properties
        def config = new MiniProfilerConfig(sysProps, null)

        expect:
        config.getProperty('a', (Integer) 99) == 1
        config.getProperty('b', (Integer) 99) == null
        config.getProperty('c', (Integer) 99) == 99
    }

    void 'handles booleans correctly'() {
        given:
        def sysProps = ['miniprofiler.a': 'true', 'miniprofiler.b': 'false'] as Properties
        def config = new MiniProfilerConfig(sysProps, null)

        expect:
        config.getProperty('a', false) == true
        config.getProperty('b', true) == false
        config.getProperty('c', false) == false
        config.getProperty('c', true) == true
    }

    void 'handles enums correctly'() {
        given:
        def sysProps = ['miniprofiler.a': 'foo', 'miniprofiler.b': 'BAR'] as Properties
        def config = new MiniProfilerConfig(sysProps, null)

        expect:
        config.getProperty('a', TestEnum, TestEnum.BAZ) == TestEnum.FOO
        config.getProperty('b', TestEnum, TestEnum.BAZ) == TestEnum.BAR
        config.getProperty('c', TestEnum, TestEnum.BAZ) == TestEnum.BAZ
    }

    void 'accepts null marker strings'() {
        given:
        def sysProps = ['miniprofiler.a': '', 'miniprofiler.b': 'none', 'miniprofiler.c': 'NULL'] as Properties
        def fileProps = ['a': '2', 'b': '2'] as Properties
        def config = new MiniProfilerConfig(sysProps, fileProps)

        expect: 'system null-marker overrides file value'
        config.getProperty('a', 'default') == null
        config.getProperty('b', 'default') == null
        config.getProperty('c', 'default') == null
    }

    void 'system property takes precedence over file property'() {
        given:
        def sysProps = ['miniprofiler.a': '1'] as Properties
        def fileProps = ['a': '2'] as Properties
        def config = new MiniProfilerConfig(sysProps, fileProps)

        expect:
        config.getProperty('a', 'default') == '1'
    }

    void 'falls back to file property when system property absent'() {
        given:
        def config = new MiniProfilerConfig(new Properties(), ['a': '2'] as Properties)

        expect:
        config.getProperty('a', 'default') == '2'
    }

    void 'null fileProps does not cause errors'() {
        given:
        def config = new MiniProfilerConfig(new Properties(), null)

        expect:
        config.getProperty('a', 'default') == 'default'
    }
}
