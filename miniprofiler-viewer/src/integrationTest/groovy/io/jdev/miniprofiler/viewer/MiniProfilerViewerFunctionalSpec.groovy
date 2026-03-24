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

package io.jdev.miniprofiler.viewer

import geb.spock.GebReportingSpec
import io.jdev.miniprofiler.test.pages.MiniProfilerSingleResultPage

class MiniProfilerViewerFunctionalSpec extends GebReportingSpec {

    Process viewerProcess

    void setup() {
        def jarPath = System.getProperty('miniprofiler.viewer.jar')
        viewerProcess = new ProcessBuilder('java', '-jar', jarPath, ViewerTestFixtures.PROFILE_FILE.absolutePath)
            .start()
        def line = new BufferedReader(new InputStreamReader(viewerProcess.inputStream)).readLine()
        def url = line.replaceFirst(/^View profile at: /, '')
        def port = new URI(url).port
        browser.baseUrl = "http://localhost:${port}/"
    }

    void cleanup() {
        viewerProcess?.destroy()
    }

    void "profile file is loaded and displayed in the browser"() {
        when:
        go "miniprofiler/results?id=${ViewerTestFixtures.PROFILE_ID}"

        then:
        at MiniProfilerSingleResultPage

        and:
        driver.title ==~ /\/test-request.* - Profiling Results/

        and:
        waitFor { page.items.size() >= 1 }
    }
}
