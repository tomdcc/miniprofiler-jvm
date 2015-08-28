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

import ratpack.func.Action;
import ratpack.handling.Chain;

/**
 * A handler chain that serves up miniprofiler results and resources for the UI.
 */
public class MiniProfilerHandlerChain implements Action<Chain> {

    /** The default URI prefix that this handler chain should be installed under. */
    public static final String DEFAULT_PREFIX = "miniprofiler";

    /**
     * Installs handlers to return results and serve UI resources on the given chain.
     * @param chain the chain to install the handler on
     * @throws Exception any
     */
    @Override
    public void execute(Chain chain) throws Exception {
        chain.post("results", MiniProfilerResultsHandler.class);
        chain.get(":path", MiniProfilerResourceHandler.class);
    }
}
