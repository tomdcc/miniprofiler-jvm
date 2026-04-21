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

package io.jdev.miniprofiler.glassfish4.funtest

import io.jdev.miniprofiler.integtest.TestMiniProfilerHttpClient
import spock.lang.Shared
import spock.lang.Specification

class Glassfish4ScenarioSpec extends Specification {

    @Shared
    String baseUrl = System.getProperty("scenarioTest.baseUrl") ?: 'http://127.0.0.1:8080/'

    @Shared
    TestMiniProfilerHttpClient client = new TestMiniProfilerHttpClient(baseUrl, 'admin/miniprofiler')

    void "profiling data returned for request"() {
        when: 'hit the home page'
        def response = client.get('')

        then: 'response is OK with profiler IDs header'
        response.statusCode() == 200
        response.miniProfilerIds().size() == 1

        when: 'fetch the profiler result as JSON'
        def resultResponse = client.awaitResultsJson(response.miniProfilerId())
        def profiler = resultResponse.bodyAsJson()

        then: 'profiler has expected timing structure'
        resultResponse.statusCode() == 200
        profiler.Name == '/'
        profiler.Root.Name == '/'

        and: 'EJB interceptor wraps service call'
        profiler.Root.Children.size() == 1
        profiler.Root.Children[0].Name == 'PersonServiceImpl.getAllPeople'

        and: 'service call has expected children'
        def serviceChildren = profiler.Root.Children[0].Children
        serviceChildren.size() == 2
        serviceChildren[0].Name == 'First thing'
        serviceChildren[1].Name == 'Second thing'

        and: 'Second thing has SQL custom timing'
        def secondThing = serviceChildren[1]
        secondThing.CustomTimings != null
        def sqlTimings = secondThing.CustomTimings.values().flatten()
        sqlTimings.size() == 1
        sqlTimings[0].CommandString =~ /(?i)SELECT\s+ID,\s*FIRSTNAME,\s*LASTNAME\s+FROM\s+PERSON/
    }
}
