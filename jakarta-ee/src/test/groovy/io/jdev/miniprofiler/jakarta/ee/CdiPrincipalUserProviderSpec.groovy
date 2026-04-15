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

package io.jdev.miniprofiler.jakarta.ee

import spock.lang.Specification

class CdiPrincipalUserProviderSpec extends Specification {

    void "locator has order 40"() {
        expect:
        new CdiPrincipalUserProviderLocator().order == 40
    }

    void "locator returns a provider when the CDI API is on the classpath"() {
        expect:
        new CdiPrincipalUserProviderLocator().locate().present
    }

    void "provider returns null when no CDI container is active"() {
        // The classpath has the CDI API but no SeContainer is bootstrapped in this unit test,
        // so CDI.current() throws and the provider must return null without propagating.
        expect:
        new CdiPrincipalUserProvider().user == null
    }
}
