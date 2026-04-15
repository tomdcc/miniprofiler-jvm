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

package io.jdev.miniprofiler.sql;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import io.jdev.miniprofiler.format.CommandFormatter;
import io.jdev.miniprofiler.internal.ConfigHelper.PropertiesWithPrefix;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static io.jdev.miniprofiler.internal.ConfigHelper.getProperty;
import static io.jdev.miniprofiler.internal.ConfigHelper.loadPropertiesFile;

/**
 * A {@link CommandFormatter} for SQL commands. Delegates formatting to
 * {@link SqlFormatter}.
 *
 * <p>Configuration is read from system properties (with prefix {@code miniprofiler.})
 * and {@code /miniprofiler.properties} on the classpath. Recognised keys (without
 * the system-property prefix) are:</p>
 * <ul>
 *   <li>{@code sql.format.dialect} &mdash; name of a {@link Dialect}, default {@code StandardSql}</li>
 *   <li>{@code sql.format.indent.width} &mdash; integer number of spaces per indent level, default {@code 2}</li>
 *   <li>{@code sql.format.uppercase} &mdash; boolean, uppercase keywords, default {@code false}</li>
 *   <li>{@code sql.format.lines.between.queries} &mdash; integer, default {@code 1}</li>
 *   <li>{@code sql.format.max.column.length} &mdash; integer, default {@code 50}</li>
 * </ul>
 */
public class SqlCommandFormatter implements CommandFormatter {

    private static final String SYSTEM_PROP_PREFIX = "miniprofiler.";
    private static final String MINIPROFILER_RESOURCE_NAME = "/miniprofiler.properties";

    private final SqlFormatter.Formatter formatter;
    private final FormatConfig formatConfig;

    /**
     * Creates a new instance, loading configuration from system properties and
     * {@code /miniprofiler.properties} on the classpath.
     */
    public SqlCommandFormatter() {
        this(System.getProperties(), loadPropertiesFile(MINIPROFILER_RESOURCE_NAME));
    }

    SqlCommandFormatter(Properties systemProps, Properties propsFileProps) {
        // system props >> miniprofiler.properties >> defaults
        List<PropertiesWithPrefix> propsList = new ArrayList<PropertiesWithPrefix>(2);
        propsList.add(new PropertiesWithPrefix(systemProps, SYSTEM_PROP_PREFIX));
        if (propsFileProps != null) {
            propsList.add(new PropertiesWithPrefix(propsFileProps, ""));
        }

        Dialect dialect = getProperty(propsList, "sql.format.dialect", Dialect.class, Dialect.StandardSql);
        this.formatter = SqlFormatter.of(dialect);
        int indentWidth = getProperty(propsList, "sql.format.indent.width", Integer.valueOf(2));
        this.formatConfig = FormatConfig.builder()
            .indent(spaces(indentWidth))
            .uppercase(getProperty(propsList, "sql.format.uppercase", false))
            .linesBetweenQueries(getProperty(propsList, "sql.format.lines.between.queries", Integer.valueOf(1)))
            .maxColumnLength(getProperty(propsList, "sql.format.max.column.length", Integer.valueOf(FormatConfig.DEFAULT_COLUMN_MAX_LENGTH)))
            .build();
    }

    private static String spaces(int n) {
        char[] chars = new char[n];
        java.util.Arrays.fill(chars, ' ');
        return new String(chars);
    }

    @Override
    public String format(String command) {
        return formatter.format(command, formatConfig);
    }

    @Override
    public boolean supports(String type) {
        return "sql".equals(type);
    }
}
