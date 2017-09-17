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
import ratpack.form.Form;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Response;

import javax.inject.Inject;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A Ratpack {@link Handler} that serves MiniProfiler results to the UI.
 */
public class MiniProfilerResultsHandler implements Handler {
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

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
        ctx.parse(Form.class).onError(e -> {
            // not a posted form
            response.status(400).send();
        }).then(form -> {
            String id = form.get("id");
            if (id == null) {
                // not provided
                response.status(400).send();
                return;
            }
            if (id.matches("\\[.+\\]")) {
                id = id.substring(1, id.length() - 1);
            }
            if (!UUID_PATTERN.matcher(id).matches()) {
                // badly formed
                response.status(400).send();
                return;
            }

            UUID uuid = UUID.fromString(id);

            Profiler profiler = provider.getStorage().load(uuid);
            if (profiler != null) {
                response.status(200).contentType("text/json").send(profiler.asUiJson());
            } else {
                // not there
                response.status(404).send();
            }
        });
    }
}
