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

import io.jdev.miniprofiler.util.ResourceHelper;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Response;

/**
 * A Ratpack {@link Handler} that serves MiniProfiler UI resources to the web browser.
 */
public class MiniProfilerResourceHandler implements Handler {

    private final ResourceHelper resourceHelper = new ResourceHelper();

    /**
     * Serves MiniProfiler UI resources to the front end
     * @param ctx the current context
     * @throws Exception any
     */
    @Override
    public void handle(Context ctx) throws Exception {
        Response response = ctx.getResponse();
        String path = ctx.getPathTokens().get("path");
        ResourceHelper.Resource resource = path != null ? resourceHelper.getResource(path) : null;
        if(resource == null) {
            response.status(404).send();
        } else {
            response.status(200).contentType(resource.getContentType()).send(resource.getContent());
        }
    }
}
