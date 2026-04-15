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

package io.jdev.miniprofiler.jakarta.servlet

import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

import java.security.Principal

class ServletUserProviderSpec extends Specification {

    ServletUserProvider provider = new ServletUserProvider()

    void cleanup() {
        ServletRequestHolder.clear()
    }

    void "returns null when no request is bound to the thread"() {
        expect:
        provider.user == null
    }

    void "returns the principal name when set"() {
        given:
        def request = new MockHttpServletRequest()
        request.userPrincipal = { 'alice' } as Principal
        ServletRequestHolder.set(request)

        expect:
        provider.user == 'alice'
    }

    void "falls back to remote user when no principal is set"() {
        given:
        def request = new MockHttpServletRequest()
        request.remoteUser = 'bob'
        ServletRequestHolder.set(request)

        expect:
        provider.user == 'bob'
    }

    void "prefers principal name over remote user when both are set"() {
        given:
        def request = new MockHttpServletRequest()
        request.userPrincipal = { 'alice' } as Principal
        request.remoteUser = 'bob'
        ServletRequestHolder.set(request)

        expect:
        provider.user == 'alice'
    }

    void "falls back to remote user when principal name is empty"() {
        given:
        def request = new MockHttpServletRequest()
        request.userPrincipal = { '' } as Principal
        request.remoteUser = 'bob'
        ServletRequestHolder.set(request)

        expect:
        provider.user == 'bob'
    }

    void "returns null when neither principal nor remote user is set"() {
        given:
        ServletRequestHolder.set(new MockHttpServletRequest())

        expect:
        provider.user == null
    }

    void "ServletUserProviderLocator returns a provider"() {
        expect:
        new ServletUserProviderLocator().locate().present
    }

    void "ServletUserProviderLocator order is below javax fallback"() {
        expect:
        new ServletUserProviderLocator().order == 20
    }
}
