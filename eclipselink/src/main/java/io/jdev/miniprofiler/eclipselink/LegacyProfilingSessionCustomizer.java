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

package io.jdev.miniprofiler.eclipselink;

import io.jdev.miniprofiler.ProfilerProvider;
import org.eclipse.persistence.sessions.Session;

/**
 * A {@link org.eclipse.persistence.config.SessionCustomizer} for EclipseLink 2.x
 * and 3.x, where the {@code org.eclipse.persistence.config.SessionCustomizer}
 * interface is the primary customizer contract.
 *
 * <p>For EclipseLink 4.x and later, use {@link ProfilingSessionCustomizer} which
 * implements the newer {@code org.eclipse.persistence.sessions.SessionCustomizer}
 * interface.</p>
 *
 * @see ProfilingSessionCustomizer
 */
@SuppressWarnings({"deprecation", "removal"})
public class LegacyProfilingSessionCustomizer implements org.eclipse.persistence.config.SessionCustomizer {

    private final ProfilerProvider profilerProvider;

    public LegacyProfilingSessionCustomizer() {
        this.profilerProvider = null;
    }

    public LegacyProfilingSessionCustomizer(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
    }

    @Override
    public void customize(Session session) throws Exception {
        SessionCustomizerSupport.customize(profilerProvider, session);
    }
}
