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
import io.jdev.miniprofiler.json.JsonUtil
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
			if(!passedIn) {
				MiniProfiler.profilerProvider = profilerProvider
			}
			def pp = passedIn ? profilerProvider : MiniProfiler

        when: 'start profiler'
            def name = 'My Function'

            Profiler profiler = pp.start(name, ProfileLevel.Verbose)

        then: 'current profiler is the one returned'
            pp.currentProfiler == profiler

		and: 'other thread has no current profiler'
			executorService.submit( { pp.currentProfiler } ).get() == null

		when: 'add some stuff'
            Timing firstTiming = profiler.step("fooService.whatever")
            Timing childTiming = profiler.step("child")
            profiler.addQueryTiming("select * from foo", 5L)
			Thread.sleep(5)
            childTiming.stop()
            Timing trivialTiming = profiler.step("trivial")
            trivialTiming.stop()
            firstTiming.stop()

        and: 'stop profiler'
			profiler.stop()

        then: 'no current profiler anymore'
            pp.currentProfiler == NullProfiler.INSTANCE

        and: 'ask for some data'
            def saved = storage.load(profiler.id)

        then: 'exists'
            saved

        when: 'convert it into json'
            def jsonString = JsonUtil.toJson(saved)
            def json = new JsonSlurper().parseText(jsonString)

        then: 'get expected result'
            json.Id == profiler.id as String
            json.MachineName == 'super server'
            json.User == 'tom'
            json.DurationMilliseconds instanceof Number
            json.Started ==~ /\/Date\(\d+\)/
            json.HasTrivialTimings == true
            json.HasAllTrivialTimings == false
            json.HasSqlTimings == true
            json.HasDuplicateSqlTimings == false
            json.DurationMillisecondsInSql == 5

            def root = json.Root
            verifyTiming(root, [Name: 'My Function', HasSqlTimings: false, Depth: 0, IsTrivial: false, HasChildren: true])
            root.Children.size() == 1

            def firstChild = root.Children[0]
            verifyTiming(firstChild, [Name: 'fooService.whatever', HasSqlTimings: false, Depth: 1, IsTrivial: false, HasChildren: true])
            firstChild.Children.size() == 2

            def nestedChild = firstChild.Children[0]
            verifyTiming(nestedChild, [Name: 'child', HasSqlTimings: true, Depth: 2, IsTrivial: false, HasChildren: false])
            nestedChild.SqlTimings.size() == 1

            def sql = nestedChild.SqlTimings[0]
            verifySqlTiming(sql, [FormattedCommandString: "\n    select\n        * \n    from\n        foo", ParentTimingName: "child", ExecuteType: 0])

            def trivialChild = firstChild.Children[1]
            verifyTiming(trivialChild, [Name: 'trivial', HasSqlTimings: false, Depth: 2, IsTrivial: true, HasChildren: false])
		where:
			passedIn << [true, false]
    }

    boolean verifyTiming(def timing, Map expectedValues) {
        assert timing.Id instanceof String
        assert timing.DurationMilliseconds instanceof Number
        assert timing.DurationWithoutChildrenMilliseconds instanceof Number
        assert timing.StartMilliseconds instanceof Number
        assert !timing.KeyValues
        expectedValues.each { key, value ->
            assert timing[key] == value
        }
        true
    }
    boolean verifySqlTiming(def timing, Map expectedValues) {
        assert timing.Id instanceof String
        assert timing.ParentTimingId instanceof String
        assert timing.StartMilliseconds instanceof Number
        assert timing.DurationMilliseconds instanceof Number
        assert timing.StackTraceSnippet == ''
        expectedValues.each { key, value ->
            assert timing[key] == value
        }
        true
    }
}
