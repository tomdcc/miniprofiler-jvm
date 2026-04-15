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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * SPI for locating {@link CommandFormatter} implementations. Implementations are
 * discovered via {@link ServiceLoader} and consulted in ascending {@link #getOrder()}
 * order. For a given custom timing type, the first formatter that
 * {@linkplain CommandFormatter#supports(String) supports} it wins.
 *
 * <p>A {@link NoopCommandFormatterLocator} is registered by default in
 * {@code miniprofiler-core} at order {@link #NOOP_COMMAND_FORMATTER_LOCATOR_ORDER}
 * as a fallback. An {@link io.jdev.miniprofiler.sql.SqlCommandFormatterLocator} is
 * registered at order 100 for SQL formatting.</p>
 */
public interface CommandFormatterLocator {

    /**
     * Sentinel order value reserved for {@link NoopCommandFormatterLocator}.
     */
    int NOOP_COMMAND_FORMATTER_LOCATOR_ORDER = Integer.MAX_VALUE;

    /**
     * Returns the order of this locator. Lower values are consulted first.
     *
     * @return the order value; lower values are consulted first
     */
    int getOrder();

    /**
     * Attempts to locate a {@link CommandFormatter}. Returns an empty
     * {@link Optional} if this locator is not applicable in the current environment.
     *
     * @return an {@link Optional} containing the formatter, or empty if not applicable
     */
    Optional<CommandFormatter> locate();

    /**
     * Discovers all registered {@link CommandFormatterLocator} implementations via
     * {@link ServiceLoader}, sorts them by {@link #getOrder()}, and returns the
     * first formatter that {@linkplain CommandFormatter#supports(String) supports}
     * the given type.
     *
     * <p>The {@link NoopCommandFormatterLocator} supports all types and is always
     * present as a fallback, so this method always returns a formatter when
     * {@code miniprofiler-core} is on the classpath.</p>
     *
     * @param type the custom timing type (e.g. "sql")
     * @return the formatter for the given type
     */
    static CommandFormatter findFormatter(String type) {
        List<CommandFormatterLocator> locators = new ArrayList<>();
        for (CommandFormatterLocator locator : ServiceLoader.load(CommandFormatterLocator.class)) {
            locators.add(locator);
        }
        locators.sort(Comparator.comparingInt(CommandFormatterLocator::getOrder));
        for (CommandFormatterLocator locator : locators) {
            Optional<CommandFormatter> formatter = locator.locate();
            if (formatter.isPresent() && formatter.get().supports(type)) {
                return formatter.get();
            }
        }
        throw new IllegalStateException("No CommandFormatterLocator returned a formatter for type '"
            + type + "'. Ensure miniprofiler-core is on the classpath.");
    }
}
