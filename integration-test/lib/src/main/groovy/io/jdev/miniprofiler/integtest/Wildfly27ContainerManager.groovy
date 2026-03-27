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

class Wildfly27ContainerManager implements LauncherSessionListener {

    // Shared across the entire test session
    static volatile GenericContainer<?> container
    static volatile String baseUrl

    @Override
    void launcherSessionOpened(LauncherSession session) {
        // Locate the WAR produced by the assemble task
        File war = new File(System.getProperty("integrationTest.warPath"))

        // WildFly 27 is the first WildFly release supporting Jakarta EE 10 with
        // the jakarta.* namespace. It ships with Hibernate 6. The WAR is dropped
        // into the deployments directory and picked up by WildFly's hot-deploy
        // scanner on startup. We wait for the "Deployed ROOT.war" log line.
        container = new GenericContainer<>("quay.io/wildfly/wildfly:27.0.1.Final-jdk17")
            .withExposedPorts(8080)
            .withCopyFileToContainer(
                MountableFile.forHostPath(war.absolutePath),
                "/opt/jboss/wildfly/standalone/deployments/ROOT.war"
            )
            .waitingFor(
                Wait.forLogMessage(".*Deployed.*ROOT\\.war.*\\n", 1)
                    .withStartupTimeout(Duration.ofMinutes(5))
            )

        container.start()

        baseUrl = "http://" + container.host + ":" + container.getMappedPort(8080) + "/"

        System.setProperty("geb.build.baseUrl", baseUrl)
    }

    @Override
    void launcherSessionClosed(LauncherSession session) {
        container?.stop()
    }
}
