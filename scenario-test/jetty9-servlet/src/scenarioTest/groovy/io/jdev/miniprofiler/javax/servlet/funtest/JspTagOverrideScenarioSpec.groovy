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

class JspTagOverrideScenarioSpec extends Specification {

    @Shared
    String baseUrl = System.getProperty("scenarioTest.baseUrl") ?: 'http://127.0.0.1:8080/javax-servlet/'

    @Shared
    TestMiniProfilerHttpClient client = new TestMiniProfilerHttpClient(baseUrl)

    void "can override properties on script tag"() {
        when:
        def response = client.get('?override=true')

        then: 'response is OK and contains miniprofiler script'
        response.statusCode() == 200
        response.body().contains('miniprofiler')
    }
}
