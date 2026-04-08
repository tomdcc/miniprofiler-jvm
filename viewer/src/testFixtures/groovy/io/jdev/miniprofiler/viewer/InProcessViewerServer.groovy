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

package io.jdev.miniprofiler.viewer

import io.jdev.miniprofiler.DefaultProfilerProvider
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.integtest.InProcessTestedServer

/**
 * An {@link InProcessTestedServer} backed by {@link MiniProfilerViewerServer},
 * using a {@link DefaultProfilerProvider}.
 */
class InProcessViewerServer implements InProcessTestedServer {

    private final DefaultProfilerProvider profilerProvider = new DefaultProfilerProvider()
    private final MiniProfilerViewerServer server

    InProcessViewerServer() {
        server = new MiniProfilerViewerServer(profilerProvider)
    }

    @Override
    ProfilerProvider getProfilerProvider() {
        profilerProvider
    }

    @Override
    String getServerUrl() {
        "http://127.0.0.1:${server.port}/"
    }

    @Override
    void close() {
        server.close()
    }
}
