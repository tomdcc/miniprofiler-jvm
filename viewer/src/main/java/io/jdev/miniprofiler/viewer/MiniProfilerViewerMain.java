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

package io.jdev.miniprofiler.viewer;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class MiniProfilerViewerMain {

    public static void main(String... args) throws Exception {
        String filePath = args.length > 0 ? args[0] : System.getProperty("miniprofiler.viewer.file");
        if (filePath == null) {
            System.err.println("Usage: miniprofiler-viewer <path-to-profile.json>");
            System.exit(1);
        }
        Path path = Paths.get(filePath);
        if (!path.toFile().isFile()) {
            System.err.println("Error: File not found: " + path);
            System.exit(1);
        }
        MiniProfilerViewerSingleFileStorage storage = MiniProfilerViewerSingleFileStorage.forFile(path);
        MiniProfilerViewerServer server = new MiniProfilerViewerServer(storage);
        System.out.println("View profile at: http://localhost:" + server.getPort()
            + MiniProfilerViewerServer.DEFAULT_PREFIX + "/results?id=" + storage.getUuid());
    }

}
