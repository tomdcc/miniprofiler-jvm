/*
 * Copyright 2015-2026 the original author or authors.
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

/*
 * Copyright 2015 the original author or authors.
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
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.server.Ids;
import ratpack.exec.Execution;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Collections;
import java.util.Optional;

/**
 * Handler which adds the miniprofiler id for the current request as a response header.
 *
 * <p>Used by the UI javascript code to display AJAX profiler info.</p>
 */
public class MiniProfilerAjaxHeaderHandler implements Handler {

    private final ProfilerProvider provider;

    /**
     * Creates a new AJAX header handler.
     *
     * @param provider the profiler provider used to look up unviewed IDs and configuration
     */
    @Inject
    public MiniProfilerAjaxHeaderHandler(ProfilerProvider provider) {
        this.provider = provider;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        Optional<Profiler> maybeProfiler = ctx.maybeGet(Profiler.class);
        if (!maybeProfiler.isPresent()) {
            ctx.next();
            return;
        }
        Profiler profiler = maybeProfiler.get();
        Execution.current().add(ProfilerStoreOption.class, ProfilerStoreOption.STORE_RESULTS);
        String user = profiler.getUser();
        if (user == null) {
            ctx.getResponse().getHeaders().add(
                "X-MiniProfiler-Ids",
                Ids.buildIdsHeader(profiler.getId(), Collections.emptyList(), 0)
            );
            ctx.next();
        } else {
            AsyncStorage asyncStorage = AsyncStorage.adapt(provider.getStorage());
            int max = provider.getUiConfig().getMaxUnviewedProfiles();
            asyncStorage.getUnviewedIdsAsync(user)
                .then(unviewedIds -> {
                    ctx.getResponse().getHeaders().add(
                        "X-MiniProfiler-Ids",
                        Ids.buildIdsHeader(profiler.getId(), unviewedIds, max)
                    );
                    ctx.next();
                });
        }
    }

}
