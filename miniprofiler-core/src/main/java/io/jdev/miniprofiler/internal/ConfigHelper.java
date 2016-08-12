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

package io.jdev.miniprofiler.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ConfigHelper {

    private static final Set<String> NULL_VALUES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "none",
        "null",
        ""
    )));

    public static String getProperty(List<PropertiesWithPrefix> props, String key, String defaultValue) {
        Result r = getProperty(props, key);
        return r != null ? r.value : defaultValue;
    }

    public static boolean getProperty(List<PropertiesWithPrefix> props, String key, boolean defaultValue) {
        Result r = getProperty(props, key);
        return r != null ? Boolean.parseBoolean(r.value) : defaultValue;
    }

    public static Integer getProperty(List<PropertiesWithPrefix> props, String key, Integer defaultValue) {
        Result r = getProperty(props, key);
        return r != null ? (r.value == null ? null : Integer.valueOf(r.value)) : defaultValue;
    }

    public static <T extends Enum<T>> T getProperty(List<PropertiesWithPrefix> props, String key, Class<T> enumClass, T defaultValue) {
        Result r = getProperty(props, key);
        return r != null ? (r.value == null ? null : Enum.valueOf(enumClass, r.value.toUpperCase())) : defaultValue;
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

    public static class PropertiesWithPrefix {
        public final Properties props;
        public final String prefix;

        public PropertiesWithPrefix(Properties properties, String prefix) {
            this.props = properties;
            this.prefix = prefix;
        }
    }

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
