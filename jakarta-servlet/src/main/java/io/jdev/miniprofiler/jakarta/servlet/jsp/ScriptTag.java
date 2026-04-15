/*
 * Copyright 2018-2026 the original author or authors.
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

package io.jdev.miniprofiler.jakarta.servlet.jsp;

import io.jdev.miniprofiler.MiniProfiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerUiConfig;
import io.jdev.miniprofiler.ScriptTagWriter;
import io.jdev.miniprofiler.internal.ConfigHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 * JSP tag that writes out a HTML script tag to load MiniProfiler resources.
 */
@SuppressWarnings("WeakerAccess")
public class ScriptTag extends TagSupport {

    /** The profiler provider used to retrieve the current profiler. */
    private ProfilerProvider profilerProvider;
    /** Writer used to render the script tag. */
    private ScriptTagWriter scriptTagWriter;

    /** The path to the MiniProfiler resources. */
    private String path;

    /** The UI widget position. */
    private ProfilerUiConfig.Position position;
    /** The UI color scheme. */
    private ProfilerUiConfig.ColorScheme colorScheme;
    /** The keyboard shortcut to toggle the UI. */
    private String toggleShortcut;
    /** The maximum number of traces to show. */
    private Integer maxTraces;
    /** The threshold in milliseconds below which timings are considered trivial. */
    private Integer trivialMilliseconds;
    /** Whether trivial timings are shown by default. */
    private Boolean trivial;
    /** Whether child timings are shown by default. */
    private Boolean children;
    /** Whether the controls panel is shown. */
    private Boolean controls;
    /** Whether the current user is authorized to view profiling results. */
    private Boolean authorized;
    /** Whether the MiniProfiler UI starts hidden. */
    private Boolean startHidden;

    /** Creates a new instance backed by the current global {@link io.jdev.miniprofiler.ProfilerProvider}. */
    public ScriptTag() {
        setProfilerProvider(MiniProfiler.getProfilerProvider());
    }

    // for testing
    ScriptTag(ProfilerProvider profilerProvider, ScriptTagWriter scriptTagWriter) {
        this.profilerProvider = profilerProvider;
        this.scriptTagWriter = scriptTagWriter;
    }

    @Override
    public int doEndTag() throws JspException {
        String contextPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath();
        try {
            pageContext.getOut().write(getContent(contextPath));
        } catch (IOException e) {
            throw new JspException(e);
        }
        return EVAL_PAGE;
    }

    // for testing
    String getContent(String contextPath) {
        return scriptTagWriter.printScriptTag(profilerProvider.current(), config(), path(contextPath));
    }

    private String path(String contextPath) {
        return this.path != null ? this.path : contextPath + profilerProvider.getUiConfig().getPath();
    }

    private ProfilerUiConfig config() {
        ProfilerUiConfig config = profilerProvider.getUiConfig().copy();
        if (position != null) {
            config.setPosition(position);
        }
        if (colorScheme != null) {
            config.setColorScheme(colorScheme);
        }
        if (toggleShortcut != null) {
            config.setToggleShortcut(toggleShortcut);
        }
        if (maxTraces != null) {
            config.setMaxTraces(maxTraces);
        }
        if (trivialMilliseconds != null) {
            config.setTrivialMilliseconds(trivialMilliseconds);
        }
        if (trivial != null) {
            config.setTrivial(trivial);
        }
        if (children != null) {
            config.setChildren(children);
        }
        if (controls != null) {
            config.setControls(controls);
        }
        if (authorized != null) {
            config.setAuthorized(authorized);
        }
        if (startHidden != null) {
            config.setStartHidden(startHidden);
        }
        return config;
    }

    /**
     * Sets the {@link ProfilerProvider} that supplies the current profiler.
     *
     * @param profilerProvider the profiler provider to use
     */
    public void setProfilerProvider(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
        this.scriptTagWriter = new ScriptTagWriter(profilerProvider);
    }

    /**
     * Sets an explicit path override for MiniProfiler resources; if {@code null}, the provider's configured path is used.
     *
     * @param path the path to the MiniProfiler resources
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Sets the UI display position; value is matched case-insensitively to {@link io.jdev.miniprofiler.ProfilerUiConfig.Position}.
     *
     * @param position the UI widget position
     */
    public void setPosition(String position) {
        this.position = position == null ? null : ConfigHelper.findEnum(ProfilerUiConfig.Position.class, position);
    }

    /**
     * Sets the UI color scheme; value is matched case-insensitively to {@link io.jdev.miniprofiler.ProfilerUiConfig.ColorScheme}.
     *
     * @param scheme the UI color scheme
     */
    public void setColorScheme(String scheme) {
        this.colorScheme = scheme == null ? null : ConfigHelper.findEnum(ProfilerUiConfig.ColorScheme.class, scheme);
    }

    /**
     * Sets the keyboard shortcut for toggling the MiniProfiler popup.
     *
     * @param toggleShortcut the keyboard shortcut string
     */
    public void setToggleShortcut(String toggleShortcut) {
        this.toggleShortcut = toggleShortcut;
    }

    /**
     * Sets the maximum number of trace entries to display in the popup.
     *
     * @param maxTraces the maximum number of traces
     */
    public void setMaxTraces(Integer maxTraces) {
        this.maxTraces = maxTraces;
    }

    /**
     * Sets the threshold in milliseconds below which timings are considered trivial.
     *
     * @param trivialMilliseconds the trivial threshold in milliseconds
     */
    public void setTrivialMilliseconds(Integer trivialMilliseconds) {
        this.trivialMilliseconds = trivialMilliseconds;
    }

    /**
     * Sets whether trivial timings are shown by default.
     *
     * @param trivial true to show trivial timings by default
     */
    public void setTrivial(boolean trivial) {
        this.trivial = trivial;
    }

    /**
     * Sets whether child timings are expanded by default.
     *
     * @param children true to show child timings by default
     */
    public void setChildren(boolean children) {
        this.children = children;
    }

    /**
     * Sets whether control buttons are shown in the popup.
     *
     * @param controls true to show control buttons
     */
    public void setControls(boolean controls) {
        this.controls = controls;
    }

    /**
     * Sets whether the current user is authorized to view profiling results.
     *
     * @param authorized true if the user is authorized
     */
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    /**
     * Sets whether the MiniProfiler popup starts in the hidden state.
     *
     * @param startHidden true to start the UI hidden
     */
    public void setStartHidden(boolean startHidden) {
        this.startHidden = startHidden;
    }

}
