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

import io.jdev.miniprofiler.integtest.TestedServer

/**
 * A {@link TestedServer} that launches the viewer shadow JAR as a subprocess.
 * The server URL is read from the first line of the process stdout.
 */
class ForkingViewerServer implements TestedServer {

    private final Process process
    private final String serverUrl

    ForkingViewerServer(String jarPath, File profileFile) {
        process = new ProcessBuilder('java', '-jar', jarPath, profileFile.absolutePath).start()
        def line = new BufferedReader(new InputStreamReader(process.inputStream)).readLine()
        def url = line.replaceFirst(/^View profile at: /, '')
        def port = new URI(url).port
        serverUrl = "http://localhost:${port}/"
    }

    @Override
    String getServerUrl() { serverUrl }

    @Override
    void close() {
        process?.destroy()
    }
}
