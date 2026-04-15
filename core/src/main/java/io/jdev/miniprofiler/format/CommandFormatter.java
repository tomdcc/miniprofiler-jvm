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

package io.jdev.miniprofiler.format;

/**
 * Formats command strings for display in the MiniProfiler UI.
 *
 * <p>Each formatter declares which custom timing types it supports via
 * {@link #supports(String)}. Implementations are discovered through
 * {@link CommandFormatterLocator} and cached per type on the
 * {@link io.jdev.miniprofiler.ProfilerProvider}.</p>
 */
public interface CommandFormatter {

    /**
     * Returns the formatted version of the given command string.
     *
     * @param command the command string to format
     * @return the formatted command string
     */
    String format(String command);

    /**
     * Returns whether this formatter supports the given custom timing type.
     *
     * @param type the custom timing type (e.g. "sql")
     * @return true if this formatter can format commands of the given type
     */
    boolean supports(String type);
}
