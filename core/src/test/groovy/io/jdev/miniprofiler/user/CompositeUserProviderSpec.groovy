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

package io.jdev.miniprofiler.user

import spock.lang.Specification

class CompositeUserProviderSpec extends Specification {

    void "returns the result from the first provider when present"() {
        given:
        def first = { 'alice' } as UserProvider
        def second = { throw new AssertionError('should not be called') } as UserProvider

        expect:
        new CompositeUserProvider([first, second]).user == 'alice'
    }

    void "falls through to next provider when earlier returns null"() {
        given:
        def first = { null } as UserProvider
        def second = { 'bob' } as UserProvider

        expect:
        new CompositeUserProvider([first, second]).user == 'bob'
    }

    void "treats empty string as no answer and falls through"() {
        given:
        def first = { '' } as UserProvider
        def second = { 'carol' } as UserProvider

        expect:
        new CompositeUserProvider([first, second]).user == 'carol'
    }

    void "returns null when every provider returns null"() {
        given:
        def first = { null } as UserProvider
        def second = { null } as UserProvider

        expect:
        new CompositeUserProvider([first, second]).user == null
    }

    void "returns null for empty chain"() {
        expect:
        new CompositeUserProvider([]).user == null
    }

    void "preserves the order of providers"() {
        given:
        def first = { 'alice' } as UserProvider
        def second = { 'bob' } as UserProvider
        def composite = new CompositeUserProvider([first, second])

        expect:
        composite.providers == [first, second]
        composite.user == 'alice'
    }

    void "stops asking once a provider has answered"() {
        given:
        def callCount = 0
        def first = { callCount++; 'alice' } as UserProvider
        def second = { throw new AssertionError('must not be invoked') } as UserProvider

        when:
        new CompositeUserProvider([first, second]).user

        then:
        callCount == 1
    }

    void "rejects null providers list"() {
        when:
        new CompositeUserProvider(null)

        then:
        thrown(NullPointerException)
    }
}
