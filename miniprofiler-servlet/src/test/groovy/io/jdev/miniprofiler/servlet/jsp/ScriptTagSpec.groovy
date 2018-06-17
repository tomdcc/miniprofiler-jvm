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

import io.jdev.miniprofiler.ScriptTagWriter
import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class ScriptTagSpec extends Specification {

    def profilerProvider = new TestProfilerProvider()

    String providedPath
    String tagContent
    def writer = new ScriptTagWriter() {
        String printScriptTag(String path) {
            providedPath = path
            tagContent = super.printScriptTag(path)
            tagContent
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

    private void verify(Map<String, Object> expectedValues, String result) {
        assert result == tagContent
        def expectedPath = expectedValues.remove('path')
        if (expectedPath) {
            assert providedPath == expectedPath
        }
    }

}
