/*
 * Copyright 2023 the original author or authors.
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

package io.jdev.miniprofiler.server;

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerUiConfig;
import io.jdev.miniprofiler.ScriptTagWriter;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.Storage;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class Pages {

    public static String renderSingleResultPage(Profiler profiler, ProfilerProvider provider, Optional<String> path) {
        StringBuilder out = new StringBuilder();
        out.append("<html>");
        out.append("<head>");
        out.append("<title>");
        out.append(profiler.getName()).append(" (").append(profiler.getRoot().getDurationMilliseconds()).append(" ms) - Profiling Results");
        out.append("</title>");
        // TODO custom styles?
        // TODO escape for html
        out.append("<script>var profiler = ").append(profiler.asUiJson()).append(";</script>");
        out.append(new ScriptTagWriter(provider).printScriptTag(profiler, resolvePath(provider, path)));
        out.append("</head>");
        out.append("<body><div class=\"mp-result-full\"></div></body>");
        out.append("</html>");
        return out.toString();
    }

    public static String renderResultListPage(ProfilerProvider provider, Optional<String> path) {
        ProfilerUiConfig config = provider.getUiConfig();
        String resolvedPath = resolvePath(provider, path);
        ScriptTagWriter scriptTagWriter = new ScriptTagWriter(provider);
        StringBuilder out = new StringBuilder();
        out.append("<!DOCTYPE html><html><head><title>List of profiling sessions</title>");
        out.append(scriptTagWriter.printListScriptTag(config, resolvedPath));
        out.append("<style>.mp-results-index { width: 950px; margin: 25px auto; }</style>");
        out.append("</head><body>");
        out.append("<table class='mp-results-index'><thead><tr>");
        out.append("<th>Name</th><th>Server</th><th>Started</th><th>Total Duration</th>");
        out.append("<th>Request Start</th><th>Response Start</th><th>Dom Complete</th>");
        out.append("</tr></thead><tbody></tbody></table>");
        out.append(scriptTagWriter.printListInitScript(config, resolvedPath));
        out.append("</body></html>");
        return out.toString();
    }

    public static String renderResultListJson(Collection<UUID> ids, Storage storage) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (UUID id : ids) {
            ProfilerImpl profiler = storage.load(id);
            if (profiler != null) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(profiler.asListJson());
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String resolvePath(ProfilerProvider provider, Optional<String> path) {
        String resolved = path.orElseGet(() -> provider.getUiConfig().getPath());
        if (!resolved.endsWith("/")) {
            resolved += "/";
        }
        return resolved;
    }

    private Pages() {
    }
}
