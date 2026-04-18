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

class IdsSpec extends Specification {

    static final UUID ID = UUID.fromString('12345678-1234-1234-1234-123456789abc')
    static final String JSON_BODY = """{"Id": "${ID}"}"""
    static final String APPLICATION_JSON = 'application/json'
    static final String TEXT_HTML = 'text/html'

    void "parses id from JSON body when Accept is application/json"() {
        expect:
        Ids.parseId(APPLICATION_JSON, JSON_BODY, null) == ID
    }

    void "parses id from JSON body when Accept contains application/json among other types"() {
        expect:
        Ids.parseId("$TEXT_HTML, $APPLICATION_JSON", JSON_BODY, null) == ID
    }

    void "falls back to id param when JSON body is valid JSON but has no Id field"() {
        expect:
        Ids.parseId(APPLICATION_JSON, '{"foo": "bar"}', ID.toString()) == ID
    }

    void "falls back to id param when JSON body is malformed JSON"() {
        expect:
        Ids.parseId(APPLICATION_JSON, 'not-json', ID.toString()) == ID
    }

    void "falls back to id param when JSON body is null"() {
        expect:
        Ids.parseId(APPLICATION_JSON, null, ID.toString()) == ID
    }

    void "falls back to id param when JSON body is empty"() {
        expect:
        Ids.parseId(APPLICATION_JSON, '', ID.toString()) == ID
    }

    void "parses id from id param when Accept is not JSON"() {
        expect:
        Ids.parseId(TEXT_HTML, null, ID.toString()) == ID
    }

    void "parses id from id param when Accept is null"() {
        expect:
        Ids.parseId(null, null, ID.toString()) == ID
    }

    void "ignores JSON body when Accept is not JSON"() {
        expect:
        // Body is valid JSON but Accept is text/html, so body is ignored
        Ids.parseId(TEXT_HTML, JSON_BODY, ID.toString()) == ID
    }

    void "returns null when all inputs are null"() {
        expect:
        Ids.parseId(null, null, null) == null
    }

    void "returns null when id param is null and no JSON body"() {
        expect:
        Ids.parseId(APPLICATION_JSON, null, null) == null
    }

    void "returns null when id param is empty"() {
        expect:
        Ids.parseId(null, null, '') == null
    }

    void "returns null when id param is not a valid UUID"() {
        expect:
        Ids.parseId(null, null, 'not-a-uuid') == null
    }

    // --- buildIdsHeader(UUID, Collection<UUID>, int) ---

    void "buildIdsHeader contains only current id when unviewed list is empty"() {
        expect:
        Ids.buildIdsHeader(ID, [], 10) == "[\"${ID}\"]"
    }

    void "buildIdsHeader includes unviewed ids"() {
        given:
        def other1 = UUID.randomUUID()
        def other2 = UUID.randomUUID()

        when:
        def header = Ids.buildIdsHeader(ID, [other1, other2], 10)

        then:
        header == "[\"${ID}\",\"${other1}\",\"${other2}\"]"
    }

    void "buildIdsHeader caps at maxUnviewedProfiles"() {
        given:
        def others = (1..5).collect { UUID.randomUUID() }

        when:
        def header = Ids.buildIdsHeader(ID, others, 2)

        then:
        def ids = header.replaceAll('[\\[\\]"]', '').split(',')
        ids.length == 3
        ids[0] == ID.toString()
    }

    void "buildIdsHeader excludes current id from unviewed list"() {
        given:
        def other = UUID.randomUUID()

        when:
        def header = Ids.buildIdsHeader(ID, [ID, other], 10)

        then:
        header == "[\"${ID}\",\"${other}\"]"
    }

    void "buildIdsHeader with zero max returns only current id"() {
        given:
        def other = UUID.randomUUID()

        expect:
        Ids.buildIdsHeader(ID, [other], 0) == "[\"${ID}\"]"
    }
}
