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

package io.jdev.miniprofiler.scenariotest

import io.jdev.miniprofiler.integtest.TestedServer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.MountableFile

import java.security.MessageDigest
import java.time.Duration

class DockerWildfly10Server implements TestedServer {

    // Shared across the entire test session
    static volatile GenericContainer<?> container
    static volatile String baseUrl

    DockerWildfly10Server() {
        // Locate the WAR produced by the assemble task
        File war = new File(System.getProperty("scenarioTest.warPath"))

        // The WAR is dropped into the deployments directory and picked up by WildFly's
        // hot-deploy scanner on startup. We wait for the "Deployed ROOT.war" log line
        // rather than a plain HTTP 200, which could match the welcome page before our
        // WAR loads.
        //
        // Provision a test user in the default ApplicationRealm before WildFly boots so
        // the BASIC-auth-protected endpoint is reachable as soon as the WAR deploys.
        // application-users.properties expects an MD5 hex digest of "user:realm:password".
        String userHash = md5Hex("alice:ApplicationRealm:secret")
        container = new GenericContainer<>(System.getProperty("dockerImage.wildfly10"))
            .withExposedPorts(8080)
            .withCopyToContainer(
                Transferable.of("alice=" + userHash + "\n"),
                "/opt/jboss/wildfly/standalone/configuration/application-users.properties"
            )
            .withCopyToContainer(
                Transferable.of("alice=user\n"),
                "/opt/jboss/wildfly/standalone/configuration/application-roles.properties"
            )
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

        System.setProperty("scenarioTest.baseUrl", baseUrl)
    }

    private static String md5Hex(String input) {
        StringBuilder sb = new StringBuilder()
        for (byte b : MessageDigest.getInstance("MD5").digest(input.bytes)) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
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
