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

/**
 * A no-op {@link CommandFormatter} that returns the command unchanged.
 * Supports all types and acts as the fallback when no other formatter matches.
 */
public class NoopCommandFormatter implements CommandFormatter {

    /** Shared singleton instance. */
    public static final NoopCommandFormatter INSTANCE = new NoopCommandFormatter();

    /** Creates a new instance. */
    public NoopCommandFormatter() {
    }

    @Override
    public String format(String command) {
        return command;
    }

    @Override
    public boolean supports(String type) {
        return true;
    }
}
