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
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.internal.ResultsRequest;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.http.TypedData;

import java.util.Optional;
import java.util.UUID;

import static io.jdev.miniprofiler.internal.Pages.renderSingleResultPage;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML;

/**
 * A Ratpack {@link Handler} that serves MiniProfiler results to the UI.
 */
public class MiniProfilerResultsHandler implements Handler {

    private final ProfilerProvider provider;

    @Inject
    public MiniProfilerResultsHandler(ProfilerProvider provider) {
        this.provider = provider;
    }

    /**
     * Serves MiniProfiler results to the UI.
     * @param ctx the current context
     * @throws Exception any
     */
    @Override
    public void handle(Context ctx) throws Exception {
        ctx.byMethod(spec -> spec
            .get(this::handleRequest)
            .post(this::handleRequest)
        );
    }

    public void handleRequest(Context ctx) {
        Response response = ctx.getResponse();
        boolean jsonRequest = isJsonRequest(ctx);

        ctx.getRequest().getBody()
            .map(TypedData::getText)
            .then(body -> {
                UUID requestedId = parseId(ctx, jsonRequest, body);
                if (requestedId == null) {
                    response.status(Status.BAD_REQUEST).send();
                    return;
                }

                AsyncStorage.adapt(provider.getStorage())
                    .loadAsync(requestedId)
                    .then(profiler -> {
                        if (profiler != null) {
                            if (isJsonRequest(ctx)) {
                                renderJson(ctx, profiler);
                            } else {
                                renderSinglePageHtml(ctx, profiler);
                            }
                        } else {
                            // not there
                            response.status(Status.NOT_FOUND).send();
                        }
                    });
            });
    }

    private static UUID parseId(Context ctx, boolean jsonRequest, String body) {
        UUID id = null;
        if (jsonRequest) {
            try {
                id = ResultsRequest.from(body).id;
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        if (id == null) {
            try {
                id = Optional.ofNullable(ctx.getRequest().getQueryParams().get("id"))
                    .map(UUID::fromString)
                    .orElse(null);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return id;
    }

    private boolean isJsonRequest(Context ctx) {
        return Optional.ofNullable(ctx.getRequest().getHeaders().get(ACCEPT))
            .map(header -> header.contains(APPLICATION_JSON)).orElse(false);
    }

    private void renderJson(Context ctx, ProfilerImpl profiler) {
        ctx.getResponse()
            .status(200)
            .contentType(APPLICATION_JSON)
            .send(profiler.asUiJson());
    }

    private void renderSinglePageHtml(Context ctx, ProfilerImpl profiler) {
        ctx.getResponse()
            .status(200)
            .contentType(TEXT_HTML)
            .send(renderSingleResultPage(profiler, provider, Optional.empty()));
    }
}
