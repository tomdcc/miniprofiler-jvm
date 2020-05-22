/*
 * Copyright 2013 the original author or authors.
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

package io.jdev.miniprofiler.ratpack.funtest

import geb.spock.GebReportingSpec
import io.jdev.miniprofiler.test.pages.MiniProfilerGapModule
import io.jdev.miniprofiler.test.pages.MiniProfilerQueryModule
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.ServerBackedApplicationUnderTest

class RatpackMiniprofilerFunctionalSpec extends GebReportingSpec {

    ServerBackedApplicationUnderTest aut

    void setup() {
        aut = new MainClassApplicationUnderTest(Main)
        browser.baseUrl = "http://${guessHostIp()}:$aut.address.port/"

        // Go here first as th`ere's an issue in the ui js the very first time it's loaded, related to something
        // being in localStorage I think. Basically the first miniprofiler load on the page is ok, but the second
        // one fails. All loads on subsequent pages are ok.
        to HomePage
        Thread.sleep(1000)

        // then reset - Geb tries to parse the about:blank and fails so we can't just go() there
        browser.js.exec('window.location = "about:blank"')
    }

    void "can see miniprofiler"() {
        when:
        to HomePage

        then: 'mini profiler visible with single timing info'
        miniProfiler
        miniProfiler.results.size() >= 1

        and: 'ajax timer also visible, eventually'
        waitFor(60) { miniProfiler.results.size() == 2 }

        and:
        verifyResults(miniProfiler.results[0])
        verifyResults(miniProfiler.results[1])
    }

    private void closeResultPopup(result) {
        if (result.popup.displayed) {
            if (result.queriesPopup.displayed) {
                result.queriesPopup.click()
            }
            result.click()
        }
    }

    private boolean verifyResults(result) {
        assert result.button.time ==~ ~/\d+\.\d ms/

        assert !result.popup.displayed

        result.button.click()
        assert waitFor { result.popup.displayed }

        def timings = result.popup.timings
        assert timings.size() == 3
        assert timings.label == ['/page', 'TestHandler.handle', 'TestHandler.getData']
        assert timings[0].duration.text() ==~ ~/\d+\.\d/
        assert !timings[0].durationWithChildren.displayed
        assert !timings[0].timeFromStart.displayed
        assert !timings[0].queries
        assert timings[2].queries.text() ==~ ~/\d+\.\d \(1\)/

        result.popup.toggleChildTimingLink.click()

        assert waitFor { timings[0].timeFromStart.displayed }
        assert timings[0].timeFromStart.text() ==~ ~/\+\d+\.\d/
        assert timings[0].durationWithChildren.displayed
        assert timings[0].durationWithChildren.text() ==~ ~/\d+\.\d/

        timings[2].queries.click()

        def queries = result.queriesPopup.queries
        assert queries.size() == 3
        assert queries[0] instanceof MiniProfilerGapModule
        assert queries[0].displayed == !queries[0].trivial
        assert queries[1] instanceof MiniProfilerQueryModule
        assert queries[1].displayed
        assert queries[2] instanceof MiniProfilerGapModule
        assert queries[2].displayed == !queries[2].trivial

        assert queries[1].step == 'TestHandler.getData'
        assert queries[1].timeFromStart ==~ ~/T\+\d+.\d ms/
        assert queries[1].duration ==~ ~/\d+.\d ms/
        assert queries[1].query ==~ ~/select\s+\*\s+from\s+people/

        closeResultPopup(result)

        true
    }

    static String guessHostIp() {
        // For the dev.gradle TC boxes, we can grab the eth0 address
        def eth0 = NetworkInterface.getByName('eth0')
        if (eth0) {
            def ipAddress = eth0.inetAddresses.find { it instanceof Inet4Address }.hostAddress
            return ipAddress
        }

        // On mac we can't use this, so find the first siteLocal address
        InetAddress siteLocalInterface = NetworkInterface.networkInterfaces
            .toList()
            .collectMany {
                it.inetAddresses.findAll { it.siteLocalAddress }
            }
            .head()

        if (siteLocalInterface) {
            def ipAddress = siteLocalInterface.hostAddress
            return ipAddress
        }

        // Otherwise, we can try to find it in the container
        def dockerIpAdress = ['docker', 'run', '--network', 'host', '--rm', 'alpine', '/bin/sh', '-c', 'getent ahosts \\$HOSTNAME']
            .execute()
            .text
            .readLines()
            .find {
                it ==~ /[0-9]{3}\..+/
            }
            .split(/\s+/)[0]
        return dockerIpAdress
    }

}
