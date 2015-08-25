/*
 * Copyright 2013 the original author or authors.
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

package io.jdev.miniprofiler;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Writes out a script tag in the format that the mini profiler front end
 * javascript expects.
 */
public class ScriptTagWriter {

    private final ProfilerProvider provider;

    public ScriptTagWriter(ProfilerProvider provider) {
        this.provider = provider;
    }

    public ScriptTagWriter() {
        this(new StaticProfilerProvider());
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * <p>Writes tag using default path to miniprofiler resources (<code>/miniprofiler</code>).</p>
     *
     * @return script html tag
     */
    public String printScriptTag() {
        // TODO: pull from config when we have some
        return printScriptTag("/miniprofiler");
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * @param path     path to the script
     * @return script html tag
     */
    public String printScriptTag(String path) {
        return printScriptTag(provider.getCurrentProfiler(), path);
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * @param profiler profiler data
     * @param path     path to the script
     * @return script html tag
     *
     * @deprecated use {@link #printScriptTag(String)} and create writer with a provider
     */
    public String printScriptTag(Profiler profiler, String path) {
        if (profiler == null || profiler == NullProfiler.INSTANCE) {
            return "";
        }
        // TODO: un-hard-code all of this stuff
        UUID currentId = profiler.getId();
        List<UUID> ids = Collections.singletonList(currentId);
        String position = "left";
        boolean showTrivial = false;
        boolean showChildren = false;
        int maxTracesToShow = 15;
        boolean showControls = false;
        boolean authorized = true;
        boolean useExistingjQuery = false;
        String version = MiniProfiler.getVersion();

        if (!path.endsWith("/")) {
            path = path + "/";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<script async type='text/javascript' id='mini-profiler'");
        appendAttribute(sb, "src", path + "includes.js?version=" + version);
        appendAttribute(sb, "data-version", version);
        appendAttribute(sb, "data-path", path);
        appendAttribute(sb, "data-current-id", currentId);
        appendAttribute(sb, "data-ids", ids.toString());
        appendAttribute(sb, "data-position", position);
        appendAttribute(sb, "data-trivial", showTrivial);
        appendAttribute(sb, "data-children", showChildren);
        appendAttribute(sb, "data-max-traces", maxTracesToShow);
        appendAttribute(sb, "data-controls", showControls);
        appendAttribute(sb, "data-authorized", authorized);
        sb.append("></script>");
        return sb.toString();
    }

    private static void appendAttribute(StringBuilder sb, String attributeName, Object value) {
        sb.append(" ").append(attributeName).append("='").append(value).append("'");
    }
}
