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

package io.jdev.miniprofiler.storage.objectstorage.azureblob

import spock.lang.Specification

class AzureBlobStorageLocatorSpec extends Specification {

    def "getOrder returns 100"() {
        expect:
        new AzureBlobStorageLocator().getOrder() == 100
    }

    def "locate returns empty when container not configured"() {
        given:
        // Ensure no Azure container system property is set
        def saved = System.getProperty("miniprofiler.storage.azure-blob.container")
        System.clearProperty("miniprofiler.storage.azure-blob.container")

        when:
        def result = new AzureBlobStorageLocator().locate()

        then:
        !result.present

        cleanup:
        if (saved != null) {
            System.setProperty("miniprofiler.storage.azure-blob.container", saved)
        }
    }
}
