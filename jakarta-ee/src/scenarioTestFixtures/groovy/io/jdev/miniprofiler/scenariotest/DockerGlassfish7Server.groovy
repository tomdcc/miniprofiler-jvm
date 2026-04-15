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
import org.testcontainers.containers.Container
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile

import java.time.Duration

class DockerGlassfish7Server implements TestedServer {

    static volatile GenericContainer<?> container
    static volatile String baseUrl

    DockerGlassfish7Server() {
        File war = new File(System.getProperty("scenarioTest.warPath"))
        File h2Jar = new File(System.getProperty("scenarioTest.h2JarPath"))

        // Wait for the GlassFish startup log message — the admin port 4848 redirects
        // to HTTPS in GlassFish 7, so we wait for the server-started log line instead.
        // Image from ghcr.io/eclipse-ee4j/glassfish (official Eclipse GlassFish images).
        container = new GenericContainer<>("ghcr.io/eclipse-ee4j/glassfish:7.0.25")
            .withExposedPorts(8080, 4848)
            // Copy H2 driver into the domain lib so it is on the server classpath
            .withCopyFileToContainer(
                MountableFile.forHostPath(h2Jar.absolutePath),
                "/opt/glassfish7/glassfish/domains/domain1/lib/h2.jar"
            )
            .waitingFor(
                Wait.forLogMessage(".*Eclipse GlassFish.*startup time.*\\n", 1)
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

            // Provision a test user in the default file realm so the scenario tests
            // can hit BASIC-auth-protected endpoints. asadmin reads the new user's
            // password from a properties file via --passwordfile, which must be passed
            // as a utility option (i.e. before the subcommand).
            // Once --passwordfile is given, asadmin stops using the local-password
            // shortcut and demands an explicit AS_ADMIN_PASSWORD. Read the local-password
            // token straight off disk so asadmin authenticates as the running server's
            // admin without us having to know any real admin credentials.
            container.execInContainer("/bin/sh", "-c",
                "printf 'AS_ADMIN_PASSWORD=%s\\nAS_ADMIN_USERPASSWORD=secret\\n' " +
                    "\"\$(cat /opt/glassfish7/glassfish/domains/domain1/config/local-password)\" > /tmp/pw.txt")
            Container.ExecResult userResult = container.execInContainer(
                "/opt/glassfish7/bin/asadmin",
                "--user", "admin",
                "--passwordfile", "/tmp/pw.txt",
                "create-file-user",
                "--groups", "user",
                "alice")
            println "asadmin create-file-user stdout: ${userResult.stdout}"
            if (userResult.stderr) {
                println "asadmin create-file-user stderr: ${userResult.stderr}"
            }
            if (userResult.exitCode != 0) {
                throw new RuntimeException(
                    "asadmin create-file-user failed (exit ${userResult.exitCode}): ${userResult.stdout} / ${userResult.stderr}")
            }

            // Now deploy the WAR at the root context
            container.copyFileToContainer(
                MountableFile.forHostPath(war.absolutePath), "/tmp/ROOT.war")
            exec("deploy",
                "--contextroot", "/",
                "--name", "ROOT",
                "/tmp/ROOT.war")

        } catch (Exception e) {
            throw new RuntimeException("Failed to configure and deploy application to GlassFish 7", e)
        }

        baseUrl = "http://" + container.host + ":" + container.getMappedPort(8080) + "/"

        // Wait for the deployed app to become accessible at the HTTP port
        // (GlassFish may serve the default page briefly after asadmin deploy returns)
        waitForApp(baseUrl)

        System.setProperty("scenarioTest.baseUrl", baseUrl)
    }

    private static void exec(String... args) {
        String[] fullCmd = new String[args.length + 3]
        fullCmd[0] = "/opt/glassfish7/bin/asadmin"
        fullCmd[1] = "--user"
        fullCmd[2] = "admin"
        System.arraycopy(args, 0, fullCmd, 3, args.length)

        def result = container.execInContainer(fullCmd)
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
    private static void waitForApp(String appBaseUrl) {
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
    String getServerUrl() {
        baseUrl
    }

    @Override
    void close() throws IOException {
        container?.stop()
    }
}
