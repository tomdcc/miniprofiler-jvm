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

package io.jdev.miniprofiler.format;

import java.util.Optional;

/**
 * Fallback {@link CommandFormatterLocator} that always returns a
 * {@link NoopCommandFormatter}. Registered at
 * {@link #NOOP_COMMAND_FORMATTER_LOCATOR_ORDER} so that it only matches
 * when no other formatter supports the requested type.
 */
public class NoopCommandFormatterLocator implements CommandFormatterLocator {

    /** Creates a new instance. */
    public NoopCommandFormatterLocator() {
    }

    @Override
    public int getOrder() {
        return NOOP_COMMAND_FORMATTER_LOCATOR_ORDER;
    }

    @Override
    public Optional<CommandFormatter> locate() {
        return Optional.of(NoopCommandFormatter.INSTANCE);
    }
}
