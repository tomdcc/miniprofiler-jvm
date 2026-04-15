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

class UserProviderLocatorSpec extends Specification {

    void "UnknownUserProviderLocator has order UNKNOWN_USER_PROVIDER_LOCATOR_ORDER"() {
        expect:
        new UnknownUserProviderLocator().order == UserProviderLocator.UNKNOWN_USER_PROVIDER_LOCATOR_ORDER
    }

    void "UnknownUserProviderLocator returns an UnknownUserProvider"() {
        when:
        def result = new UnknownUserProviderLocator().locate()

        then:
        result.present
        result.get() instanceof UnknownUserProvider
    }

    void "UnknownUserProvider returns null"() {
        expect:
        UnknownUserProvider.INSTANCE.getUser() == null
    }

    void "findUserProvider returns a user provider via ServiceLoader"() {
        when:
        def provider = UserProviderLocator.findUserProvider()

        then:
        provider != null
    }

    void "findUserProvider returns the unknown provider when only the fallback is registered"() {
        // The bundled META-INF/services registration only contains UnknownUserProviderLocator,
        // so the resolved chain reports no user.
        expect:
        UserProviderLocator.findUserProvider().user == null
    }
}
