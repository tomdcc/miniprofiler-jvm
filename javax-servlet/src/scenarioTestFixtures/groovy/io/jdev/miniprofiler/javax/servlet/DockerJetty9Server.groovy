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

package io.jdev.miniprofiler.javax.servlet

import io.jdev.miniprofiler.integtest.TestedServer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile

import java.time.Duration

class DockerJetty9Server implements TestedServer {

    static volatile GenericContainer<?> container
    static volatile String baseUrl

    DockerJetty9Server() {
        File war = new File(System.getProperty("scenarioTest.warPath"))

        // Deploy the WAR as javax-servlet.war so it is served at the /javax-servlet/ context root,
        // matching the context path the functional tests expect.
        container = new GenericContainer<>("jetty:9-jre11")
            .withExposedPorts(8080)
            .withCopyFileToContainer(
                MountableFile.forHostPath(war.absolutePath),
                "/var/lib/jetty/webapps/javax-servlet.war"
            )
            .waitingFor(
                Wait.forHttp("/javax-servlet/").forPort(8080).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5))
            )

        container.start()

        baseUrl = "http://" + container.host + ":" + container.getMappedPort(8080) + "/javax-servlet/"

        System.setProperty("scenarioTest.baseUrl", baseUrl)
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
