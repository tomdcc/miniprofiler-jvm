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

package io.jdev.miniprofiler.internal;

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ScriptTagWriter;

import java.util.Optional;

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
        out.append(new ScriptTagWriter(provider).printScriptTag(profiler, path.orElseGet(() -> provider.getUiConfig().getPath())));
        out.append("</head>");
        out.append("<body><div class=\"mp-result-full\"></div></body>");
        out.append("</html>");
        return out.toString();
    }

    private Pages() {
    }
}
