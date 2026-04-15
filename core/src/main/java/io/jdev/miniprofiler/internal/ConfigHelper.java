/*
 * Copyright 2016-2026 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Internal helper for reading configuration properties. */
public class ConfigHelper {

    /** Use the static methods on this class. */
    private ConfigHelper() {}

    private static final Set<String> NULL_VALUES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "none",
        "null",
        ""
    )));

    /**
     * Returns the String property value, or {@code defaultValue} if not set.
     *
     * @param props the ordered list of property sources to search
     * @param key the property key
     * @param defaultValue the value to return if the property is not found
     * @return the property value, or {@code defaultValue}
     */
    public static String getProperty(List<PropertiesWithPrefix> props, String key, String defaultValue) {
        Result r = getProperty(props, key);
        return r != null ? r.value : defaultValue;
    }

    /**
     * Returns the boolean property value, or {@code defaultValue} if not set.
     *
     * @param props the ordered list of property sources to search
     * @param key the property key
     * @param defaultValue the value to return if the property is not found
     * @return the property value, or {@code defaultValue}
     */
    public static boolean getProperty(List<PropertiesWithPrefix> props, String key, boolean defaultValue) {
        Result r = getProperty(props, key);
        return r != null ? Boolean.parseBoolean(r.value) : defaultValue;
    }

    /**
     * Returns the Integer property value, or {@code defaultValue} if not set.
     *
     * @param props the ordered list of property sources to search
     * @param key the property key
     * @param defaultValue the value to return if the property is not found
     * @return the property value, or {@code defaultValue}
     */
    public static Integer getProperty(List<PropertiesWithPrefix> props, String key, Integer defaultValue) {
        Result r = getProperty(props, key);
        return r != null ? (r.value == null ? null : Integer.valueOf(r.value)) : defaultValue;
    }

    /**
     * Returns the enum property value, or {@code defaultValue} if not set.
     *
     * @param <T> the enum type
     * @param props the ordered list of property sources to search
     * @param key the property key
     * @param enumClass the enum class to convert the value to
     * @param defaultValue the value to return if the property is not found
     * @return the property value, or {@code defaultValue}
     */
    public static <T extends Enum<T>> T getProperty(List<PropertiesWithPrefix> props, String key, Class<T> enumClass, T defaultValue) {
        Result r = getProperty(props, key);
        return r != null ? (r.value == null ? null : findEnum(enumClass, r.value)) : defaultValue;
    }

    /**
     * Finds the enum constant matching {@code value}, case-insensitively.
     *
     * @param <T> the enum type
     * @param enumClass the enum class to search
     * @param value the string to match
     * @return the matching enum constant
     */
    public static <T extends Enum<T>> T findEnum(Class<T> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            // try looking for case insensitive match
            return Arrays.stream(enumClass.getEnumConstants())
                .filter(c -> c.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> e);
        }
    }

    private static class Result {
        final String value;

        private Result(String value) {
            this.value = value;
        }
    }

    private static Result getProperty(List<PropertiesWithPrefix> propsList, String key) {
        for (PropertiesWithPrefix props : propsList) {
            String value = props.props.getProperty(props.prefix + key);
            if (value != null) {
                value = value.trim();
                return new Result(NULL_VALUES.contains(value.toLowerCase()) ? null : value);
            }
        }
        return null;
    }

    /** A {@link Properties} instance paired with a prefix to apply to all key lookups. */
    public static class PropertiesWithPrefix {
        /** The underlying properties. */
        public final Properties props;
        /** The key prefix applied to all lookups in this properties instance. */
        public final String prefix;

        /**
         * Creates a new instance.
         *
         * @param properties the underlying properties
         * @param prefix the key prefix to apply to all lookups
         */
        public PropertiesWithPrefix(Properties properties, String prefix) {
            this.props = properties;
            this.prefix = prefix;
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
            Properties miniprofilerResourceProps = new Properties();
            miniprofilerResourceProps.load(resourceStream);
            return miniprofilerResourceProps;
        } catch (Exception e) {
            // just ignore, not a props file, but let the user know that miniprofiler.properties doesn't
            // look legit
            e.printStackTrace();
            return null;
        } finally {
            try {
                resourceStream.close();
            } catch (IOException e) {
                // not much else we can do
                e.printStackTrace();
            }
        }
    }

}
