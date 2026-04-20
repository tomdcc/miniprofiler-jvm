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

package io.jdev.miniprofiler.server

import spock.lang.Specification

class ResultsRequestSpec extends Specification {

    void "body without Performance field produces null clientTimings"() {
        given:
        def id = UUID.randomUUID().toString()

        when:
        def req = ResultsRequest.from("""{"Id":"${id}"}""")

        then:
        req.id == UUID.fromString(id)
        req.clientTimings == null
    }

    void "body with Performance array produces non-null clientTimings"() {
        given:
        def id = UUID.randomUUID().toString()
        def body = """{"Id":"${id}","Performance":[{"Name":"fetchStart","Start":0,"Duration":12},{"Name":"firstPaintTime","Start":380}]}"""

        when:
        def req = ResultsRequest.from(body)

        then:
        req.clientTimings != null
        req.clientTimings.size() == 2
    }

    void "body with Performance array preserves span and point timing values"() {
        given:
        def id = UUID.randomUUID().toString()
        def body = """{"Id":"${id}","Performance":[{"Name":"fetchStart","Start":0,"Duration":12},{"Name":"firstPaintTime","Start":380}]}"""

        when:
        def req = ResultsRequest.from(body)

        then:
        def span = req.clientTimings[0]
        span.name == "fetchStart"
        span.start == 0L
        span.duration == 12L

        def point = req.clientTimings[1]
        point.name == "firstPaintTime"
        point.start == 380L
        point.duration == null
    }

    void "body with empty Performance array produces null clientTimings"() {
        given:
        def id = UUID.randomUUID().toString()

        when:
        def req = ResultsRequest.from("""{"Id":"${id}","Performance":[]}""")

        then:
        req.clientTimings == null
    }
}
