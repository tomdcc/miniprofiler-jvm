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

package io.jdev.miniprofiler.user;

/**
 * A {@link UserProvider} that always returns {@code null}, indicating
 * that the current user is unknown. This is the default fallback when
 * no other user provider is configured.
 */
public class UnknownUserProvider implements UserProvider {

    /** Shared singleton instance. */
    public static final UnknownUserProvider INSTANCE = new UnknownUserProvider();

    /** Creates a new instance. */
    public UnknownUserProvider() {
    }

    @Override
    public String getUser() {
        return null;
    }
}
