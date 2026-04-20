/*
 * Copyright 2013-2026 the original author or authors.
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

import io.jdev.miniprofiler.internal.NullProfiler;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Writes out a script tag in the format that the mini profiler front end
 * javascript expects.
 */
public class ScriptTagWriter {

    private final ProfilerProvider provider;

    /**
     * Creates a new script tag writer using the given profiler provider.
     *
     * @param provider the profiler provider to use
     */
    public ScriptTagWriter(ProfilerProvider provider) {
        this.provider = provider;
    }

    /**
     * Creates a new script tag writer using a default {@link StaticProfilerProvider}.
     */
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
        return printScriptTag(provider.current(), provider.getUiConfig());
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * <p>Writes tag using default path to miniprofiler resources (<code>/miniprofiler</code>).</p>
     *
     * @param config specific UI config
     * @return script html tag
     */
    public String printScriptTag(ProfilerUiConfig config) {
        return printScriptTag(provider.current(), config);
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * @param path     path to the script
     * @return script html tag
     */
    public String printScriptTag(String path) {
        return printScriptTag(provider.current(), path);
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * @param profiler the profiler to use when writing the tag
     * @return script html tag
     */
    public String printScriptTag(Profiler profiler) {
        return printScriptTag(profiler, provider.getUiConfig());
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * @param profiler profiler data
     * @param path     path to the script
     * @return script html tag
     */
    public String printScriptTag(Profiler profiler, String path) {
        if (profiler == null || profiler == NullProfiler.INSTANCE) {
            return "";
        }
        return printScriptTag(profiler, provider.getUiConfig(), path);
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * @param profiler profiler data
     * @param config   specific UI config
     * @return script html tag
     */
    public String printScriptTag(Profiler profiler, ProfilerUiConfig config) {
        return printScriptTag(profiler, config, config.getPath());
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects.
     *
     * @param profiler profiler data
     * @param config   specific UI config
     * @param path     path to the script
     * @return script html tag
     */
    public String printScriptTag(Profiler profiler, ProfilerUiConfig config, String path) {
        if (profiler == null || profiler == NullProfiler.INSTANCE) {
            return "";
        }
        UUID currentId = profiler.getId();
        List<UUID> ids = Collections.singletonList(currentId);
        String version = MiniProfiler.getVersion();

        if (!path.endsWith("/")) {
            path = path + "/";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<script type='text/javascript' id='mini-profiler'");
        appendAttribute(sb, "src", path + "includes.js?version=" + version);
        appendAttribute(sb, "data-path", path);
        appendAttribute(sb, "data-version", version);

        appendAttribute(sb, "data-current-id", currentId);
        appendAttribute(sb, "data-ids", ids.stream().map(UUID::toString).collect(Collectors.joining(",")));

        appendAttribute(sb, "data-position", config.getPosition().name());
        if (config.getToggleShortcut() != null) {
            appendAttribute(sb, "data-toggle-shortcut", config.getToggleShortcut());
        }
        if (config.getMaxTraces() != null) {
            appendAttribute(sb, "data-max-traces", config.getMaxTraces());
        }
        if (config.getTrivialMilliseconds() != null) {
            appendAttribute(sb, "data-trivial-milliseconds", config.getTrivialMilliseconds());
        }

        appendAttribute(sb, "data-trivial", config.isTrivial());
        appendAttribute(sb, "data-children", config.isChildren());
        appendAttribute(sb, "data-controls", config.isControls());
        appendAttribute(sb, "data-authorized", config.isAuthorized());
        appendAttribute(sb, "data-start-hidden", config.isStartHidden());
        appendAttribute(sb, "data-scheme", config.getColorScheme());
        sb.append("></script>");
        return sb.toString();
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects for the results list page.
     *
     * <p>Writes tag using default path to miniprofiler resources (<code>/miniprofiler</code>).</p>
     *
     * @return script html tag
     */
    public String printListScriptTag() {
        return printListScriptTag(provider.getUiConfig());
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects for the results list page.
     *
     * @param path path to the script
     * @return script html tag
     */
    public String printListScriptTag(String path) {
        return printListScriptTag(provider.getUiConfig(), path);
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects for the results list page.
     *
     * @param config specific UI config
     * @return script html tag
     */
    public String printListScriptTag(ProfilerUiConfig config) {
        return printListScriptTag(config, config.getPath());
    }

    /**
     * Writes out a script tag in the format that the mini profiler front end
     * javascript expects for the results list page.
     *
     * @param config specific UI config
     * @param path   path to the script
     * @return script html tag
     */
    public String printListScriptTag(ProfilerUiConfig config, String path) {
        String version = MiniProfiler.getVersion();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<script id='mini-profiler'");
        appendAttribute(sb, "src", path + "includes.js?version=" + version);
        appendAttribute(sb, "data-version", version);
        appendAttribute(sb, "data-path", path);
        appendAttribute(sb, "data-color-scheme", config.getColorScheme().name());
        sb.append("></script>");
        return sb.toString();
    }

    /**
     * Writes out an inline script tag that invokes <code>MiniProfiler.listInit()</code> for the results list page.
     *
     * <p>Uses default path to miniprofiler resources (<code>/miniprofiler</code>).</p>
     *
     * @return inline script html tag
     */
    public String printListInitScript() {
        return printListInitScript(provider.getUiConfig());
    }

    /**
     * Writes out an inline script tag that invokes <code>MiniProfiler.listInit()</code> for the results list page.
     *
     * @param path path to miniprofiler resources
     * @return inline script html tag
     */
    public String printListInitScript(String path) {
        return printListInitScript(provider.getUiConfig(), path);
    }

    /**
     * Writes out an inline script tag that invokes <code>MiniProfiler.listInit()</code> for the results list page.
     *
     * @param config specific UI config
     * @return inline script html tag
     */
    public String printListInitScript(ProfilerUiConfig config) {
        return printListInitScript(config, config.getPath());
    }

    /**
     * Writes out an inline script tag that invokes <code>MiniProfiler.listInit()</code> for the results list page.
     *
     * @param config specific UI config
     * @param path   path to miniprofiler resources
     * @return inline script html tag
     */
    public String printListInitScript(ProfilerUiConfig config, String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        String version = MiniProfiler.getVersion();
        return "<script>MiniProfiler.listInit({path: '" + path
            + "', version: '" + version
            + "', authorized: '" + config.isAuthorized()
            + "', colorScheme: '" + config.getColorScheme().name() + "'});</script>";
    }

    // TODO: We don't currently encode these attributes properly. The only case where this is likely to be a problem
    // is the toggleShortcut where someone could put a
    private static void appendAttribute(StringBuilder sb, String attributeName, Object value) {
        sb.append(" ").append(attributeName).append("='").append(value).append("'");
    }
}
