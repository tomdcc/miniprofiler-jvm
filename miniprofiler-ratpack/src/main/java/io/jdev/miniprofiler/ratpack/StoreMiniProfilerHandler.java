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

import ratpack.exec.Execution;
import ratpack.handling.Context;
import ratpack.handling.Handler;

/**
 * Handler that binds {@link ProfilerStoreOption#STORE_RESULTS} to the current execution,
 * to signal that the current profiler should be saved.
 *
 * <p>Useful to turn on profile storage for chains that are interesting, when the default
 * is to have them switched off.</p>
 */
public class StoreMiniProfilerHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        Execution.current().add(ProfilerStoreOption.class, ProfilerStoreOption.STORE_RESULTS);
        ctx.next();
    }
}
