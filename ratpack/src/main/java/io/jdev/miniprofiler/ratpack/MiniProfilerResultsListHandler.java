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
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.server.Pages;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.storage.Storage;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

/**
 * A Ratpack {@link Handler} that serves MiniProfiler results list as JSON.
 */
public class MiniProfilerResultsListHandler implements Handler {

    private final ProfilerProvider provider;

    /**
     * Creates a new instance using the given profiler provider.
     *
     * @param provider the profiler provider to use
     */
    @Inject
    public MiniProfilerResultsListHandler(ProfilerProvider provider) {
        this.provider = provider;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        AsyncStorage asyncStorage = AsyncStorage.adapt(provider.getStorage());

        asyncStorage.listAsync(100, null, null, Storage.ListResultsOrder.Descending)
            .flatMap(ids -> applyLastId(ctx, asyncStorage, ids))
            .then(ids -> {
                String json = Pages.renderResultListJson(ids, provider.getStorage());
                ctx.getResponse()
                    .status(200)
                    .contentType(APPLICATION_JSON)
                    .send(json);
            });
    }

    private Promise<Collection<UUID>> applyLastId(Context ctx, AsyncStorage asyncStorage, Collection<UUID> ids) {
        String lastIdParam = ctx.getRequest().getQueryParams().get("last-id");
        if (lastIdParam == null || lastIdParam.isEmpty()) {
            return Promise.value(ids);
        }
        UUID lastId;
        try {
            lastId = UUID.fromString(lastIdParam);
        } catch (IllegalArgumentException e) {
            return Promise.value(ids);
        }
        return asyncStorage.loadAsync(lastId).map(lastProfiler -> {
            if (lastProfiler == null) {
                return ids;
            }
            long cutoff = lastProfiler.getStarted();
            List<UUID> filtered = new ArrayList<>();
            for (UUID id : ids) {
                ProfilerImpl p = provider.getStorage().load(id);
                if (p != null && p.getStarted() > cutoff) {
                    filtered.add(id);
                }
            }
            return filtered;
        });
    }
}
