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

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.internal.ResultsRequest;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Response;
import ratpack.http.TypedData;

import javax.inject.Inject;

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
        Response response = ctx.getResponse();

        ctx.getRequest().getBody()
            .map(TypedData::getText)
            .then(body -> {
                ResultsRequest resultsRequest;
                try {
                    resultsRequest = ResultsRequest.from(body);
                } catch (Exception e) {
                    response.status(400).send();
                    return;
                }

                Profiler profiler = provider.getStorage().load(resultsRequest.id);
                if (profiler != null) {
                    response.status(200).contentType("text/json").send(profiler.asUiJson());
                } else {
                    // not there
                    response.status(404).send();
                }
            });
    }
}
