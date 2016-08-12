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

import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class ScriptTagWriterSpec extends Specification {

    ProfilerUiConfig config = ProfilerUiConfig.defaults()
    TestProfilerProvider provider = new TestProfilerProvider(uiConfig: config)
    ScriptTagWriter writer = new ScriptTagWriter(provider)
    Profiler profiler = provider.start('foo')

    void "writer writes full config"() {
        given:
        config.toggleShortcut = 'shortcut'
        config.trivialMilliseconds = 20

        when:
        def result = writer.printScriptTag()

        then:
        def attrs = parseTag(result)
        attrs.src == "$config.path/includes.js?version=${MiniProfiler.version}"
        attrs['data-path'] == "$config.path/"
        attrs['data-version'] == MiniProfiler.version

        and:
        attrs['data-current-id'] == profiler.id.toString()
        attrs['data-ids'] == "[$profiler.id]"

        and:
        attrs['data-position'] == config.position.name().toLowerCase()
        attrs['data-toggle-shortcut'] == config.toggleShortcut
        attrs['data-max-traces'] == config.maxTraces as String

        and:
        attrs['data-trivial'] == config.trivial as String
        attrs['data-trivial-milliseconds'] == config.trivialMilliseconds as String
        attrs['data-children'] == config.children as String
        attrs['data-controls'] == config.controls as String
        attrs['data-authorized'] == config.authorized as String
        attrs['data-start-hidden'] == config.startHidden as String
    }

    void "writer omits attributes for null config values"() {
        given:
        config.maxTraces = null
        config.toggleShortcut = null
        config.trivialMilliseconds = null

        when:
        def result = writer.printScriptTag()

        then:
        def attrs = parseTag(result)
        !attrs.containsKey('data-max-traces')
        !attrs.containsKey('data-toggle-shortcut')
        !attrs.containsKey('data-trivial-milliseconds')
    }

    void "can override path for backwards compatibility"() {
        when:
        def result = writer.printScriptTag('/foo')

        then:
        def attrs = parseTag(result)
        attrs.src == "/foo/includes.js?version=${MiniProfiler.version}"
        attrs['data-path'] == "/foo/"
    }

    void "can pass in specific profiler for backwards compat"() {
        given:
        profiler.stop()
        // start a different profiling session
        provider.start('bar')

        when:
        def result = writer.printScriptTag(profiler, '/foo')

        then:
        def attrs = parseTag(result)
        attrs.src == "/foo/includes.js?version=${MiniProfiler.version}"
        attrs['data-path'] == "/foo/"
        attrs['data-current-id'] == profiler.id.toString()
        attrs['data-ids'] == "[$profiler.id]"
    }

    private static Map<String, String> parseTag(String tag) {
        def match = tag =~ /<script type='text\/javascript' id='mini-profiler' src='(.*?)'(.*)><\/script>/
        assert match.matches()
        // this is obviously q&d, but it works for our test data well enough
        [src: match.group(1)] + (match.group(2).split(' data-') as List).findAll { it }.collectEntries { item ->
            def itemMatch = item =~ /(.*)='(.*)'/
            assert itemMatch.matches()
            ["data-${itemMatch.group(1)}".toString(), itemMatch.group(2)]
        }
    }
}
