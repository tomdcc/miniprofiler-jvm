/*
 * Copyright 2014 the original author or authors.
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
import org.eclipse.persistence.sessions.SessionCustomizer;

/**
 * An EclipseLink SessionCustomizer that installs a
 * {@link ProfilingConnector} onto EclipseLink's read and write pools.
 *
 * <p>The customizer can be installed by setting the
 * <code>eclipselink.session.customizer</code> property to this class name
 * <code>io.jdev.miniprofiler.eclipselink.ProfilingSessionCustomizer</code>
 * </p>
 */
public class ProfilingSessionCustomizer implements SessionCustomizer {

    private final ProfilerProvider profilerProvider;

    /** Creates a new instance using the static {@link io.jdev.miniprofiler.MiniProfiler} profiler provider. */
    public ProfilingSessionCustomizer() {
        this.profilerProvider = null;
    }

    /**
     * Creates a new instance using the given profiler provider.
     *
     * @param profilerProvider the profiler provider to use
     */
    public ProfilingSessionCustomizer(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
    }

    @Override
    public void customize(Session session) throws Exception {
        SessionCustomizerSupport.customize(profilerProvider, session);
    }
}
