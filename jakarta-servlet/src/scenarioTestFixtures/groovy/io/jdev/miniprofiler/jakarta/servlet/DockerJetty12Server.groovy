/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdev.miniprofiler.jakarta.servlet

import io.jdev.miniprofiler.integtest.TestedServer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable

import java.time.Duration

class DockerJetty12Server implements TestedServer {

    static volatile GenericContainer<?> container
    static volatile String baseUrl

    DockerJetty12Server() {
        File war = new File(System.getProperty("integrationTest.warPath"))

        // Deploy the WAR as jakarta-servlet.war so it is served at the /jakarta-servlet/ context root,
        // matching the context path the functional tests expect.
        // Jetty 12 requires ee10-deploy and ee10-jsp modules to be explicitly enabled
        // for Jakarta EE 10 WAR deployment and JSP compilation.
        container = new GenericContainer<>("jetty:12.0-jre21")
            .withExposedPorts(8080)
            .withCopyToContainer(
                Transferable.of("--module=ee10-deploy\n--module=ee10-jsp\n"),
                "/var/lib/jetty/start.d/ee10.ini"
            )
            .withCopyToContainer(
                Transferable.of(war.bytes),
                "/var/lib/jetty/webapps/jakarta-servlet.war"
            )
            .waitingFor(
                Wait.forHttp("/jakarta-servlet/").forPort(8080).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5))
            )

        container.start()

        baseUrl = "http://" + container.host + ":" + container.getMappedPort(8080) + "/jakarta-servlet/"

        System.setProperty("geb.build.baseUrl", baseUrl)
    }

    @Override
    String getServerUrl() {
        baseUrl
    }

    @Override
    void close() throws IOException {
        container?.stop()
    }
}
