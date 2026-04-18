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

package io.jdev.miniprofiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Reads MiniProfiler configuration from system properties and a
 * {@code /miniprofiler.properties} file on the classpath.
 *
 * <p>For each property key, system properties are checked first (with the prefix
 * {@code miniprofiler.}), then the classpath file (with the key used as-is).
 * For example, asking for {@code "storage.s3.bucket"} checks the system property
 * {@code miniprofiler.storage.s3.bucket} and then the file key {@code storage.s3.bucket}.</p>
 *
 * <p>A value of {@code ""}, {@code "none"}, or {@code "null"} (case-insensitive) is
 * treated as an explicit {@code null}.</p>
 */
public class MiniProfilerConfig {

    private static final String SYSTEM_PROP_PREFIX = "miniprofiler.";
    private static final String RESOURCE_NAME = "/miniprofiler.properties";

    private static final Set<String> NULL_VALUES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "none", "null", ""
    )));

    private final Properties systemProps;
    private final Properties fileProps;

    /**
     * Creates a new instance that reads from the real system properties and
     * {@code /miniprofiler.properties} on the classpath.
     */
    public MiniProfilerConfig() {
        this(System.getProperties(), loadPropertiesFile(RESOURCE_NAME));
    }

    /**
     * Creates a new instance with explicit property sources.
     *
     * @param systemProps the system properties to consult first
     * @param fileProps   the file properties to consult as fallback, or {@code null}
     */
    public MiniProfilerConfig(Properties systemProps, Properties fileProps) {
        this.systemProps = systemProps;
        this.fileProps = fileProps;
    }

    /**
     * Returns the String property value, or {@code defaultValue} if not set.
     *
     * @param key          the property key (without the {@code miniprofiler.} prefix)
     * @param defaultValue the value to return if the property is not found
     * @return the property value, or {@code defaultValue}
     */
    public String getProperty(String key, String defaultValue) {
        Found f = find(key);
        return f != null ? f.value : defaultValue;
    }

    /**
     * Returns the boolean property value, or {@code defaultValue} if not set.
     *
     * @param key          the property key (without the {@code miniprofiler.} prefix)
     * @param defaultValue the value to return if the property is not found
     * @return the property value, or {@code defaultValue}
     */
    public boolean getProperty(String key, boolean defaultValue) {
        Found f = find(key);
        return f != null ? Boolean.parseBoolean(f.value) : defaultValue;
    }

    /**
     * Returns the Integer property value, or {@code defaultValue} if not set.
     * A null-marker value ({@code ""}, {@code "none"}, {@code "null"}) returns {@code null}.
     *
     * @param key          the property key (without the {@code miniprofiler.} prefix)
     * @param defaultValue the value to return if the property is not found
     * @return the property value, or {@code defaultValue}
     */
    public Integer getProperty(String key, Integer defaultValue) {
        Found f = find(key);
        return f != null ? (f.value == null ? null : Integer.valueOf(f.value)) : defaultValue;
    }

    /**
     * Returns the int property value, or {@code defaultValue} if not set or set to a null marker.
     *
     * @param key          the property key (without the {@code miniprofiler.} prefix)
     * @param defaultValue the value to return if the property is not found or is a null marker
     * @return the property value, or {@code defaultValue}
     */
    public int getProperty(String key, int defaultValue) {
        Found f = find(key);
        return (f != null && f.value != null) ? Integer.parseInt(f.value) : defaultValue;
    }

    /**
     * Returns the enum property value, or {@code defaultValue} if not set.
     * Matching is case-insensitive. A null-marker value returns {@code null}.
     *
     * @param <T>          the enum type
     * @param key          the property key (without the {@code miniprofiler.} prefix)
     * @param enumClass    the enum class to convert the value to
     * @param defaultValue the value to return if the property is not found
     * @return the property value, or {@code defaultValue}
     */
    public <T extends Enum<T>> T getProperty(String key, Class<T> enumClass, T defaultValue) {
        Found f = find(key);
        return f != null ? (f.value == null ? null : findEnum(enumClass, f.value)) : defaultValue;
    }

    private Found find(String key) {
        String value = systemProps.getProperty(SYSTEM_PROP_PREFIX + key);
        if (value == null && fileProps != null) {
            value = fileProps.getProperty(key);
        }
        if (value == null) {
            return null;
        }
        value = value.trim();
        return new Found(NULL_VALUES.contains(value.toLowerCase()) ? null : value);
    }

    private static class Found {
        final String value;

        Found(String value) {
            this.value = value;
        }
    }

    /**
     * Finds the enum constant matching {@code value}, case-insensitively.
     *
     * @param <T>       the enum type
     * @param enumClass the enum class to search
     * @param value     the string to match
     * @return the matching enum constant
     */
    public static <T extends Enum<T>> T findEnum(Class<T> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return Arrays.stream(enumClass.getEnumConstants())
                .filter(c -> c.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> e);
        }
    }

    /**
     * Loads a properties file from the classpath, returning null if not found.
     *
     * @param resourceName the classpath resource name of the properties file
     * @return the loaded properties, or null if the resource does not exist
     */
    public static Properties loadPropertiesFile(String resourceName) {
        InputStream resourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        if (resourceStream == null) {
            return null;
        }
        try {
            Properties props = new Properties();
            props.load(resourceStream);
            return props;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                resourceStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
