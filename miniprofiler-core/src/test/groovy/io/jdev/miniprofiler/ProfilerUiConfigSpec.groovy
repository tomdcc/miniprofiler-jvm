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

package io.jdev.miniprofiler

import spock.lang.Specification

class ProfilerUiConfigSpec extends Specification {

    void "config has correct defaults"() {
        when:
        def config = ProfilerUiConfig.defaults()

        then:
        with(config) {
            path == '/miniprofiler'
            position == ProfilerUiConfig.Position.RIGHT
            toggleShortcut == null
            maxTraces == 15
            trivialMilliseconds == null
            trivial == false
            children == false
            controls == false
            authorized == true
            startHidden == false
        }

        when:
        ProfilerUiConfig emptyConfig = ProfilerUiConfig.create([:] as Properties, null)

        then:
        config == emptyConfig
    }

    void "config can be overridden by system props and default property file resource"() {
        given:
        def systemProps = [
            'miniprofiler.trivial.milliseconds': '22',
            'miniprofiler.controls': 'false',
            'miniprofiler.max.traces': ''
        ] as Properties
        def fileProps = [
            'trivial.milliseconds': '77',
            'position': 'left'
        ] as Properties

        when:
        def config = ProfilerUiConfig.create(systemProps, fileProps)

        then: 'system prop overrides file prop'
        config.trivialMilliseconds == 22

        and: 'system prop only'
        config.controls == false

        and: 'file only'
        config.position == ProfilerUiConfig.Position.LEFT

        and: 'setting to null'
        config.maxTraces == null
    }

    void "cannot directly set non-nullable properties to null"() {
        given:
        def config = ProfilerUiConfig.defaults()

        when:
        config.path = null

        then:
        thrown(IllegalArgumentException)

        when:
        config.position = null

        then:
        thrown(IllegalArgumentException)
    }

    void "cannot set non-nullable properties to null via props"() {
        when:
        ProfilerUiConfig.create([:] as Properties, [path: ''] as Properties)

        then:
        thrown(IllegalArgumentException)

        when:
        ProfilerUiConfig.create([:] as Properties, ['position': ''] as Properties)

        then:
        thrown(IllegalArgumentException)
    }

    void "config can be copied"() {
        given:
        def config = other()

        when:
        def copy = config.copy()

        then:
        with(copy) {
            path == config.path
            position == config.position
            toggleShortcut == config.toggleShortcut
            maxTraces == config.maxTraces
            trivialMilliseconds == config.trivialMilliseconds
            trivial == config.trivial
            children == config.children
            controls == config.controls
            authorized == config.authorized
            startHidden == config.startHidden
        }
    }

    private static ProfilerUiConfig other() {
        ProfilerUiConfig.create().with {
            path = '/other-path'
            position = ProfilerUiConfig.Position.BOTTOMLEFT
            toggleShortcut = 'whatever'
            maxTraces = 99
            trivialMilliseconds = 95
            trivial = true
            children = true
            controls = true
            authorized = true
            startHidden = true
            it
        }
    }

}
