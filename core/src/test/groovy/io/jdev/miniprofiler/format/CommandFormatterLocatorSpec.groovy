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

package io.jdev.miniprofiler.format

import io.jdev.miniprofiler.sql.SqlCommandFormatter
import io.jdev.miniprofiler.sql.SqlCommandFormatterLocator
import spock.lang.Specification

class CommandFormatterLocatorSpec extends Specification {

    void "NoopCommandFormatterLocator has order NOOP_COMMAND_FORMATTER_LOCATOR_ORDER"() {
        expect:
        new NoopCommandFormatterLocator().order == CommandFormatterLocator.NOOP_COMMAND_FORMATTER_LOCATOR_ORDER
    }

    void "NoopCommandFormatterLocator returns a NoopCommandFormatter"() {
        when:
        def result = new NoopCommandFormatterLocator().locate()

        then:
        result.present
        result.get() instanceof NoopCommandFormatter
    }

    void "SqlCommandFormatterLocator returns a SqlCommandFormatter"() {
        when:
        def result = new SqlCommandFormatterLocator().locate()

        then:
        result.present
        result.get() instanceof SqlCommandFormatter
    }

    void "SqlCommandFormatterLocator has lower order than NoopCommandFormatterLocator"() {
        expect:
        new SqlCommandFormatterLocator().order < new NoopCommandFormatterLocator().order
    }

    void "findFormatter returns SqlCommandFormatter for sql type"() {
        when:
        def formatter = CommandFormatterLocator.findFormatter("sql")

        then:
        formatter instanceof SqlCommandFormatter
    }

    void "findFormatter returns NoopCommandFormatter for unknown type"() {
        when:
        def formatter = CommandFormatterLocator.findFormatter("unknown")

        then:
        formatter instanceof NoopCommandFormatter
    }

    void "findFormatter returns formatter from lowest-order locator"() {
        given: 'a low-order locator returning a specific formatter'
        def expectedFormatter = Mock(CommandFormatter) {
            supports("test") >> true
        }
        def lowOrderLocator = new CommandFormatterLocator() {
            int getOrder() { return 1 }
            Optional<CommandFormatter> locate() { Optional.of(expectedFormatter) }
        }
        def highOrderLocator = new CommandFormatterLocator() {
            int getOrder() { return 99 }
            Optional<CommandFormatter> locate() { throw new AssertionError("should not be called") }
        }

        when:
        def locators = [highOrderLocator, lowOrderLocator].sort { it.order }
        def result = locators.findResult {
            def f = it.locate().orElse(null)
            f?.supports("test") ? f : null
        }

        then:
        result == expectedFormatter
    }

    void "findFormatter skips formatters that do not support the type"() {
        given:
        def unsupportedFormatter = Mock(CommandFormatter) {
            supports("test") >> false
        }
        def expectedFormatter = Mock(CommandFormatter) {
            supports("test") >> true
        }
        def unsupportedLocator = new CommandFormatterLocator() {
            int getOrder() { return 1 }
            Optional<CommandFormatter> locate() { Optional.of(unsupportedFormatter) }
        }
        def supportedLocator = new CommandFormatterLocator() {
            int getOrder() { return 2 }
            Optional<CommandFormatter> locate() { Optional.of(expectedFormatter) }
        }

        when:
        def locators = [unsupportedLocator, supportedLocator].sort { it.order }
        def result = locators.findResult {
            def f = it.locate().orElse(null)
            f?.supports("test") ? f : null
        }

        then:
        result == expectedFormatter
    }
}
