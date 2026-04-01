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
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Status;

import java.util.Optional;

import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML;

/**
 * A Ratpack {@link Handler} that serves the MiniProfiler results index page.
 */
public class MiniProfilerResultsIndexHandler implements Handler {

    private final ProfilerProvider provider;

    @Inject
    public MiniProfilerResultsIndexHandler(ProfilerProvider provider) {
        this.provider = provider;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        ctx.getResponse()
            .status(Status.OK)
            .contentType(TEXT_HTML)
            .send(Pages.renderResultListPage(provider, Optional.empty()));
    }
}
