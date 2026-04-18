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

package io.jdev.miniprofiler.storage.objectstorage.gcs

import spock.lang.Specification

class GcsStorageLocatorSpec extends Specification {

    def "getOrder returns 100"() {
        expect:
        new GcsStorageLocator().getOrder() == 100
    }

    def "locate returns empty when bucket not configured"() {
        given:
        // Ensure no GCS bucket system property is set
        def saved = System.getProperty("miniprofiler.storage.gcs.bucket")
        System.clearProperty("miniprofiler.storage.gcs.bucket")

        when:
        def result = new GcsStorageLocator().locate()

        then:
        !result.present

        cleanup:
        if (saved != null) {
            System.setProperty("miniprofiler.storage.gcs.bucket", saved)
        }
    }
}
