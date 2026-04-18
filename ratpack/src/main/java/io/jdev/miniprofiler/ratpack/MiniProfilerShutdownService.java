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

package io.jdev.miniprofiler.ratpack;

import com.google.inject.Inject;
import ratpack.service.Service;
import ratpack.service.StopEvent;

/**
 * A Ratpack {@link Service} that closes the {@link RatpackContextProfilerProvider}
 * (and its storage) when the server stops.
 *
 * <p>Register this service when your storage implementation requires explicit shutdown
 * (e.g. a storage with a background expiry thread). When using {@link MiniProfilerModule},
 * override {@link MiniProfilerModule#isShutdownStorageOnStop()} to return {@code true}
 * to have this service registered automatically.</p>
 */
public class MiniProfilerShutdownService implements Service {

    private final RatpackContextProfilerProvider provider;

    /**
     * Creates a new instance; normally called by Guice.
     *
     * @param provider the profiler provider whose storage will be closed on server stop
     */
    @Inject
    public MiniProfilerShutdownService(RatpackContextProfilerProvider provider) {
        this.provider = provider;
    }

    /**
     * Closes the provider's storage asynchronously when the server stops.
     *
     * @param event the stop event
     */
    @Override
    public void onStop(StopEvent event) {
        provider.closeAsync()
            .then();
    }
}
