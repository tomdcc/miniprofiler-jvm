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

import io.jdev.miniprofiler.storage.MapStorage
import io.jdev.miniprofiler.user.UserProvider
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DefaultProfilerProviderSpec extends Specification {

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

	def cleanup() {
		executorService.submit( { profilerProvider.stopCurrentSession(false) } )
	}

	void cleanupSpec() {
		executorService.shutdownNow()
	}

    void "stores current profiler in threadlocal"() {
        when: 'start profiler'
            Profiler profiler = profilerProvider.start('asd')

        then: 'current profiler is the one returned'
            profilerProvider.currentProfiler == profiler

		and: 'other thread has no current profiler'
			executorService.submit( { profilerProvider.currentProfiler } ).get() == null

		when: 'call stop'
			profilerProvider.stopCurrentSession(false)

		then: 'no longer available'
			profilerProvider.currentProfiler == NullProfiler.INSTANCE

		when: 'other thread creates profiler'
			def otherProfiler = executorService.submit( { profilerProvider.start('lkj') } ).get()

		then: 'not set in this thread'
			profilerProvider.currentProfiler == NullProfiler.INSTANCE

		and: 'still current in that thread'
			otherProfiler == executorService.submit( { profilerProvider.currentProfiler } ).get()
	}

}
