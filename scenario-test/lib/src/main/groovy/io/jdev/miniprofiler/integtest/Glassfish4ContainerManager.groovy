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
import org.testcontainers.containers.Container
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile

import java.time.Duration

class Glassfish4ContainerManager implements LauncherSessionListener {

    static volatile GenericContainer<?> container
    static volatile String baseUrl

    @Override
    void launcherSessionOpened(LauncherSession session) {
        File war = new File(System.getProperty("integrationTest.warPath"))
        File h2Jar = new File(System.getProperty("integrationTest.h2JarPath"))

        // Wait for the GlassFish admin port to be serving HTTP — this indicates
        // GlassFish has fully started and the asadmin commands can be issued.
        container = new GenericContainer<>("glassfish:4.1")
            .withExposedPorts(8080, 4848)
            // Copy H2 driver into the domain lib so it is on the server classpath
            .withCopyFileToContainer(
                MountableFile.forHostPath(h2Jar.absolutePath),
                "/usr/local/glassfish4/glassfish/domains/domain1/lib/h2.jar"
            )
            .waitingFor(
                Wait.forHttp("/").forPort(4848).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5))
            )

        container.start()

        try {
            // JDBC resources must be created BEFORE the WAR is deployed so that
            // EclipseLink can resolve the datasource during application startup.
            //
            // asadmin uses ':' as the property-list separator, so colons inside
            // property values must be escaped as '\:'.  H2 1.3.x JdbcDataSource
            // uses setUrl() → JavaBeans property name "url" (lowercase).
            //
            // asadmin cannot handle '=' inside property values (it is the
            // name=value separator), so DB_CLOSE_DELAY=-1 cannot be embedded in
            // the URL.  Omitting it is safe because GlassFish's connection pool
            // keeps at least one connection alive, which prevents the in-memory
            // H2 database from being closed between requests.
            String jdbcUrl = "jdbc\\:h2\\:mem\\:miniprofiler"
            exec("create-jdbc-connection-pool",
                "--datasourceclassname", "org.h2.jdbcx.JdbcDataSource",
                "--property", "url=" + jdbcUrl + ":user=sa:password=sa",
                "MiniprofilerPool")

            exec("create-jdbc-resource",
                "--connectionpoolid", "MiniprofilerPool",
                "jdbc/DataSource")

            // Now deploy the WAR at the root context
            container.copyFileToContainer(
                MountableFile.forHostPath(war.absolutePath), "/tmp/ROOT.war")
            exec("deploy",
                "--contextroot", "/",
                "--name", "ROOT",
                "/tmp/ROOT.war")

        } catch (Exception e) {
            throw new RuntimeException("Failed to configure and deploy application to GlassFish", e)
        }

        baseUrl = "http://" + container.host + ":" + container.getMappedPort(8080) + "/"

        // Wait for the deployed app to become accessible at the HTTP port
        // (GlassFish may serve the default page briefly after asadmin deploy returns)
        waitForApp(baseUrl)

        System.setProperty("geb.build.baseUrl", baseUrl)
    }

    private void exec(String... args) {
        String[] fullCmd = new String[args.length + 3]
        fullCmd[0] = "/usr/local/glassfish4/bin/asadmin"
        fullCmd[1] = "--user"
        fullCmd[2] = "admin"
        System.arraycopy(args, 0, fullCmd, 3, args.length)

        Container.ExecResult result = container.execInContainer(fullCmd)
        println "asadmin ${args[0]} stdout: ${result.stdout}"
        if (result.stderr) {
            println "asadmin ${args[0]} stderr: ${result.stderr}"
        }
        if (result.exitCode != 0) {
            throw new RuntimeException(
                "asadmin ${args[0]} failed (exit ${result.exitCode}): ${result.stdout} / ${result.stderr}")
        }
    }

    /**
     * Polls the app root until it responds with a non-default-GlassFish page,
     * giving the deployed WAR time to replace the welcome screen.
     */
    private void waitForApp(String appBaseUrl) {
        long deadline = System.currentTimeMillis() + 120_000L
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(appBaseUrl).openConnection()
                conn.connectTimeout = 2000
                conn.readTimeout = 5000
                conn.connect()
                if (conn.responseCode == 200) {
                    return
                }
            } catch (Exception ignore) {
                // server not ready yet
            }
            try {
                Thread.sleep(1000)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt()
                return
            }
        }
        throw new RuntimeException("App did not become accessible at ${appBaseUrl} within timeout")
    }

    @Override
    void launcherSessionClosed(LauncherSession session) {
        container?.stop()
    }
}
