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

package io.jdev.miniprofiler

import groovy.json.JsonSlurper
import io.jdev.miniprofiler.internal.NullProfiler
import io.jdev.miniprofiler.storage.MapStorage
import io.jdev.miniprofiler.user.UserProvider
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MiniProfilerIntegrationSpec extends Specification {

    DefaultProfilerProvider profilerProvider
    MapStorage storage
    static ExecutorService executorService

    void setupSpec() {
        executorService = Executors.newSingleThreadExecutor()
    }

    void setup() {
        storage = new MapStorage()
        profilerProvider = new DefaultProfilerProvider(storage: storage)
        profilerProvider.machineName = "super server"
        profilerProvider.userProvider = { 'tom' } as UserProvider
    }

    void cleanup() {
        MiniProfiler.profilerProvider = null
    }

    void cleanupSpec() {
        executorService.shutdownNow()
    }

    @Unroll
    void "basic functionality using passed-in profile = #passedIn"() {
        given:
        if (!passedIn) {
            MiniProfiler.profilerProvider = profilerProvider
        }
        def pp = passedIn ? profilerProvider : MiniProfiler

        when: 'start profiler'
        def name = 'My Function'

        Profiler profiler = pp.start(name, ProfileLevel.Verbose)

        then: 'current profiler is the one returned'
        pp.current == profiler

        and: 'other thread has no current profiler'
        executorService.submit({ pp.current }).get() == null

        when: 'add some stuff'
        Timing firstTiming = profiler.step("fooService.whatever")
        Timing childTiming = profiler.step("child")
        profiler.addCustomTiming('sql', 'query', 'select * from "foo"', 5L)
        Thread.sleep(5)
        childTiming.stop()
        Timing trivialTiming = profiler.step("trivial")
        trivialTiming.stop()
        firstTiming.stop()

        and: 'stop profiler'
        profiler.stop()

        then: 'no current profiler anymore'
        pp.current == NullProfiler.INSTANCE

        and: 'ask for some data'
        def saved = storage.load(profiler.id)

        then: 'exists'
        saved

        when: 'convert it into json'
        def jsonString = saved.asUiJson()
        def json = new JsonSlurper().parseText(jsonString)

        then: 'get expected result'
        json.Id == profiler.id as String
        json.MachineName == 'super server'
        json.DurationMilliseconds instanceof Number
        json.Started instanceof Number

        def root = json.Root
        verifyTiming(root, [Name: 'My Function'])
        root.Children.size() == 1

        def firstChild = root.Children[0]
        verifyTiming(firstChild, [Name: 'fooService.whatever'])
        firstChild.Children.size() == 2

        def nestedChild = firstChild.Children[0]
        verifyTiming(nestedChild, [Name: 'child'])
        nestedChild.CustomTimings.size() == 1
        nestedChild.CustomTimings.sql.size() == 1

        def sqlTiming = nestedChild.CustomTimings.sql[0]
        verifyCustomTiming(sqlTiming, [CommandString: '\n    select\n        * \n    from\n        "foo"', ExecuteType: 'query'])

        def trivialChild = firstChild.Children[1]
        verifyTiming(trivialChild, [Name: 'trivial'])
        where:
        passedIn << [true, false]
    }

    @Unroll
    void "adding open custom timings using passed-in profile = #passedIn"() {
        given:
        if (!passedIn) {
            MiniProfiler.profilerProvider = profilerProvider
        }
        def pp = passedIn ? profilerProvider : MiniProfiler

        when: 'add custom timings'
        Profiler profiler = pp.start('ignored name', ProfileLevel.Verbose)
        def ct1 = profiler.head.customTiming('the type', 'the execute type', 'the command')
        Thread.sleep(5)
        ct1.stop()
        def ct2 = profiler.customTiming('the type', 'the execute type 2', 'the command 2')
        Thread.sleep(5)
        ct2.stop()
        profiler.stop()

        then: 'has correct custom timings'
        def jsonString = profiler.asUiJson()
        def json = new JsonSlurper().parseText(jsonString)
        def customTimings = json.Root.CustomTimings
        customTimings.keySet() == ['the type'] as Set
        verifyCustomTiming(customTimings['the type'][0], [CommandString: '\n    the command', ExecuteType: 'the execute type'])
        verifyCustomTiming(customTimings['the type'][1], [CommandString: '\n    the command 2', ExecuteType: 'the execute type 2'])

        where:
        passedIn << [true, false]
    }

    boolean verifyTiming(def timing, Map expectedValues) {
        assert timing.Id instanceof String
        assert timing.DurationMilliseconds instanceof Number
        assert timing.StartMilliseconds instanceof Number
        expectedValues.each { key, value ->
            assert timing[key] == value
        }
        true
    }

    boolean verifyCustomTiming(def timing, Map expectedValues) {
        assert timing.Id instanceof String
        assert timing.StartMilliseconds instanceof Number
        assert timing.DurationMilliseconds instanceof Number
        assert timing.StackTraceSnippet == ''
        expectedValues.each { key, value ->
            assert timing[key] == value
        }
        true
    }
}
