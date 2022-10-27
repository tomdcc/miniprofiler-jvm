/*
 * Copyright 2016 the original author or authors.
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

import io.jdev.miniprofiler.internal.ConfigHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static io.jdev.miniprofiler.internal.ConfigHelper.getProperty;
import static io.jdev.miniprofiler.internal.ConfigHelper.loadPropertiesFile;

public class ProfilerUiConfig {

    public enum Position {
        Left, Right, BottomLeft, BottomRight
    }

    public enum ColorScheme {
        Light, Dark, Auto
    }

    private String path;
    private Position position;
    private ColorScheme colorScheme;
    private String toggleShortcut;
    private Integer maxTraces;
    private Integer trivialMilliseconds;
    private boolean trivial;
    private boolean children;
    private boolean controls;
    private boolean authorized;
    private boolean startHidden;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException((String) null);
        }
        this.path = path;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException((String) null);
        }
        this.position = position;
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(ColorScheme colorScheme) {
        if (colorScheme == null) {
            throw new IllegalArgumentException((String) null);
        }
        this.colorScheme = colorScheme;
    }

    public String getToggleShortcut() {
        return toggleShortcut;
    }

    public void setToggleShortcut(String toggleShortcut) {
        this.toggleShortcut = toggleShortcut;
    }

    public Integer getMaxTraces() {
        return maxTraces;
    }

    public void setMaxTraces(Integer maxTraces) {
        this.maxTraces = maxTraces;
    }

    public Integer getTrivialMilliseconds() {
        return trivialMilliseconds;
    }

    public void setTrivialMilliseconds(Integer trivialMilliseconds) {
        this.trivialMilliseconds = trivialMilliseconds;
    }

    public boolean isTrivial() {
        return trivial;
    }

    public void setTrivial(boolean trivial) {
        this.trivial = trivial;
    }

    public boolean isChildren() {
        return children;
    }

    public void setChildren(boolean children) {
        this.children = children;
    }

    public boolean isControls() {
        return controls;
    }

    public void setControls(boolean controls) {
        this.controls = controls;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean isStartHidden() {
        return startHidden;
    }

    public void setStartHidden(boolean startHidden) {
        this.startHidden = startHidden;
    }

    private ProfilerUiConfig() {}

    public static ProfilerUiConfig defaults() {
        ProfilerUiConfig config = new ProfilerUiConfig();
        config.path = "/miniprofiler";
        config.position = Position.Right;
        config.colorScheme = ColorScheme.Auto;
        config.toggleShortcut = null;
        config.maxTraces = 15;
        config.trivialMilliseconds = null;
        config.trivial = false;
        config.children = false;
        config.controls = false;
        config.authorized = true;
        config.startHidden = false;
        return config;
    }

    private static final String SYSTEM_PROP_PREFIX = "miniprofiler.";
    private static final String MINIPROFILER_RESOURCE_NAME = "/miniprofiler.properties";

    public static ProfilerUiConfig create() {
        return create(System.getProperties(), loadPropertiesFile(MINIPROFILER_RESOURCE_NAME));
    }

    private static ProfilerUiConfig create(Properties systemProps, Properties propsFileProps) {
        // system props >> miniprofiler.properties >> defaults
        List<ConfigHelper.PropertiesWithPrefix> propsList = new ArrayList<ConfigHelper.PropertiesWithPrefix>(2);
        propsList.add(new ConfigHelper.PropertiesWithPrefix(systemProps, SYSTEM_PROP_PREFIX));
        if (propsFileProps != null) {
            propsList.add(new ConfigHelper.PropertiesWithPrefix(propsFileProps, ""));
        }
        ProfilerUiConfig config = defaults();
        config.setPath(getProperty(propsList, "path", config.path));
        config.setPosition(getProperty(propsList, "position", Position.class, config.position));
        config.setColorScheme(getProperty(propsList, "color.scheme", ColorScheme.class, config.colorScheme));
        config.toggleShortcut = getProperty(propsList, "toggle.shortcut", config.toggleShortcut);
        config.maxTraces = getProperty(propsList, "max.traces", config.maxTraces);
        config.trivialMilliseconds = getProperty(propsList, "trivial.milliseconds", config.trivialMilliseconds);
        config.trivial = getProperty(propsList, "trivial", config.trivial);
        config.children = getProperty(propsList, "children", config.children);
        config.controls = getProperty(propsList, "controls", config.controls);
        config.authorized = getProperty(propsList, "authorized", config.authorized);
        config.startHidden = getProperty(propsList, "start.hidden", config.startHidden);
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProfilerUiConfig that = (ProfilerUiConfig) o;

        if (trivial != that.trivial) {
            return false;
        }
        if (children != that.children) {
            return false;
        }
        if (controls != that.controls) {
            return false;
        }
        if (authorized != that.authorized) {
            return false;
        }
        if (startHidden != that.startHidden) {
            return false;
        }
        if (!path.equals(that.path)) {
            return false;
        }
        if (position != that.position) {
            return false;
        }
        if (colorScheme != that.colorScheme) {
            return false;
        }
        if (toggleShortcut != null ? !toggleShortcut.equals(that.toggleShortcut) : that.toggleShortcut != null) {
            return false;
        }
        if (maxTraces != null ? !maxTraces.equals(that.maxTraces) : that.maxTraces != null) {
            return false;
        }
        return trivialMilliseconds != null ? trivialMilliseconds.equals(that.trivialMilliseconds) : that.trivialMilliseconds == null;

    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + position.hashCode();
        result = 31 * result + colorScheme.hashCode();
        result = 31 * result + (toggleShortcut != null ? toggleShortcut.hashCode() : 0);
        result = 31 * result + (maxTraces != null ? maxTraces.hashCode() : 0);
        result = 31 * result + (trivialMilliseconds != null ? trivialMilliseconds.hashCode() : 0);
        result = 31 * result + (trivial ? 1 : 0);
        result = 31 * result + (children ? 1 : 0);
        result = 31 * result + (controls ? 1 : 0);
        result = 31 * result + (authorized ? 1 : 0);
        result = 31 * result + (startHidden ? 1 : 0);
        return result;
    }

    /**
     * Creates a copy of this config object.
     *
     * <p>
     *     The main purpose of this is to allow overriding specific options on a case-by-case basis.
     *     Copy the main config, then override.
     * </p>
     *
     * @return a copy with the same properties
     */
    public ProfilerUiConfig copy() {
        ProfilerUiConfig copy = new ProfilerUiConfig();
        copy.path = this.path;
        copy.position = this.position;
        copy.colorScheme = this.colorScheme;
        copy.toggleShortcut = this.toggleShortcut;
        copy.maxTraces = this.maxTraces;
        copy.trivialMilliseconds = this.trivialMilliseconds;
        copy.trivial = this.trivial;
        copy.children = this.children;
        copy.controls = this.controls;
        copy.authorized = this.authorized;
        copy.startHidden = this.startHidden;
        return copy;
    }
}
