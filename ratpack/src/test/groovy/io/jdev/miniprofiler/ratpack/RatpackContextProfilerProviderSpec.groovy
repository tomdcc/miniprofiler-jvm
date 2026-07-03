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

package io.jdev.miniprofiler.ratpack

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.storage.MapStorage
import ratpack.exec.ExecController
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class RatpackContextProfilerProviderSpec extends Specification {

    @AutoCleanup
    ExecController execController = ExecController.builder().build()

    def polling = new PollingConditions(timeout: 5)

    void "constructor requires an ExecController"() {
        when:
        new RatpackContextProfilerProvider(null)

        then:
        thrown(NullPointerException)
    }

    void "profiler stopped outside of an execution is still saved, via a forked execution"() {
        given:
        def storage = new MapStorage()
        def provider = new RatpackContextProfilerProvider(execController)
        provider.storage = storage

        and: 'a profiler bound to the provider'
        def id = UUID.randomUUID()
        def profiler = new ProfilerImpl(id, 'root', 'root', ProfileLevel.Info, provider)

        when: 'stopped on the test thread, i.e. not on a managed Ratpack execution'
        profiler.stop()

        then: 'the async save still runs (the provider forked an execution to do it)'
        polling.eventually {
            storage.load(id) != null
        }
    }

    void "profiler stopped when its execution has already completed is still saved"() {
        given:
        def storage = new MapStorage()
        def provider = new RatpackContextProfilerProvider(execController)
        provider.storage = storage

        and: 'the initializer stops the profiler in the execution completion, where the execution can no longer take promises'
        UUID id = null

        when: 'a context-less execution with a bound profiler completes'
        ExecHarness.runSingle({ it.add(new MiniProfilerExecInitializer(provider)) }) { execution ->
            id = (provider.start('root') as ProfilerImpl).id
        }

        then: 'the save is recovered onto a forked execution rather than lost to "this execution has completed"'
        polling.eventually {
            storage.load(id) != null
        }
    }
}
