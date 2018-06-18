/*
 * Copyright 2018 the original author or authors.
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

package io.jdev.miniprofiler.servlet.jsp

import io.jdev.miniprofiler.ProfilerUiConfig
import io.jdev.miniprofiler.ScriptTagWriter
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class ScriptTagSpec extends Specification {

    private static final String CONTENT = '<script />'

    def profilerProvider = new TestProfilerProvider()

    String providedPath
    ProfilerUiConfig providedConfig
    def writer = Spy(ScriptTagWriter) {
        _ * printScriptTag(_, _, _) >> { profiler, config, path ->
            providedPath = path
            providedConfig = config
            CONTENT
        }
    }
    def tag = new ScriptTag(profilerProvider, writer)

    void 'appends context path to given prefix'() {
        given:
        profilerProvider.uiConfig.path = uiPath

        when:
        def result = tag.getContent(contextPath)

        then:
        verify(result, path: expectedPath)

        where:
        uiPath | contextPath | expectedPath
        ''     | ''          | ''
        '/ui'  | ''          | '/ui'
        ''     | '/ctx'      | '/ctx'
        '/ui'  | '/ctx'      | '/ctx/ui'
    }

    void 'path can be explicitly set'() {
        given:
        profilerProvider.uiConfig.path = '/uipath'
        tag.path = '/set-path'

        when:
        def result = tag.getContent('ignoredContextPath')

        then:
        verify(result, path: '/set-path')
    }

    void '#prop can be set'(String prop, Object value, Object expectedValue) {
        given:
        tag."$prop" = value

        when:
        def result = tag.getContent('/ctx')

        then:
        verify(result, (prop): expectedValue)

        where:
        prop                  | value        | expectedValue
        'position'            | 'bottomleft' | ProfilerUiConfig.Position.BOTTOMLEFT
        'toggleShortcut'      | 'foo'        | 'foo'
        'maxTraces'           | 99           | 99
        'trivialMilliseconds' | 66           | 66
        'trivial'             | true         | true
        'children'            | true         | true
        'controls'            | true         | true
        'authorized'          | false        | false
        'startHidden'         | true         | true
    }

    private void verify(Map<String, Object> expectedValues, String result) {
        assert result == CONTENT
        def expectedPath = expectedValues.remove('path')
        if (expectedPath) {
            assert providedPath == expectedPath
        }
        expectedValues.each { key, expectedValue ->
            assert providedConfig."$key" == expectedValue
        }
    }

}
