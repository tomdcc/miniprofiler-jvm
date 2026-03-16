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

package io.jdev.miniprofiler.integtest

import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile

import java.time.Duration

class JettyContainerManager implements LauncherSessionListener {

    static volatile GenericContainer<?> container
    static volatile String baseUrl

    @Override
    void launcherSessionOpened(LauncherSession session) {
        File war = new File(System.getProperty("integrationTest.warPath"))

        // Deploy the WAR as servlet.war so it is served at the /servlet/ context root,
        // matching the context path the functional tests expect.
        container = new GenericContainer<>("jetty:9-jre11")
            .withExposedPorts(8080)
            .withCopyFileToContainer(
                MountableFile.forHostPath(war.absolutePath),
                "/var/lib/jetty/webapps/servlet.war"
            )
            .waitingFor(
                Wait.forHttp("/servlet/").forPort(8080).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5))
            )

        container.start()

        baseUrl = "http://" + container.host + ":" + container.getMappedPort(8080) + "/servlet/"

        System.setProperty("geb.build.baseUrl", baseUrl)
    }

    @Override
    void launcherSessionClosed(LauncherSession session) {
        container?.stop()
    }
}
