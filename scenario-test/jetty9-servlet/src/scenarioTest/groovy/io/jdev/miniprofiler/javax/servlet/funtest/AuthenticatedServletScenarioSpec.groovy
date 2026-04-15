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

package io.jdev.miniprofiler.javax.servlet.funtest

import io.jdev.miniprofiler.integtest.TestMiniProfilerHttpClient
import spock.lang.Shared
import spock.lang.Specification

class AuthenticatedServletScenarioSpec extends Specification {

    @Shared
    String baseUrl = System.getProperty("scenarioTest.baseUrl") ?: 'http://127.0.0.1:8080/javax-servlet/'

    @Shared
    TestMiniProfilerHttpClient client = new TestMiniProfilerHttpClient(baseUrl)

    static Map<String, String> basicAuth(String user, String password) {
        ['Authorization': 'Basic ' + Base64.encoder.encodeToString("${user}:${password}".bytes)]
    }

    Map findInResultsList(String id) {
        def list = client.getResultsList().bodyAsJson() as List<Map>
        list.find { it.Id == id }
    }

    void "anonymous request to public page records no user on the profile"() {
        when:
        def response = client.get('')

        then:
        response.statusCode() == 200

        when:
        def entry = findInResultsList(response.miniProfilerId())

        then:
        entry != null
        entry.User == null
    }

    void "authenticated request to secured page records the principal name on the profile"() {
        when:
        def response = client.get('secure/hello', basicAuth('alice', 'secret'))

        then:
        response.statusCode() == 200
        response.body() == 'hello alice'

        when:
        def entry = findInResultsList(response.miniProfilerId())

        then:
        entry != null
        entry.User == 'alice'
    }

    void "request to secured page without credentials is rejected and not profiled"() {
        when:
        def response = client.get('secure/hello')

        then: 'auth filter rejects before miniprofiler runs, so no profiler header is set'
        response.statusCode() == 401
        !response.header('X-MiniProfiler-Ids').present
    }
}
