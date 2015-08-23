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

public class MiniProfilerHandlerChain implements Action<Chain> {
    public static final String DEFAULT_PREFIX = "miniprofiler";

    @Override
    public void execute(Chain chain) throws Exception {
        chain.post("results", new MiniProfilerResultsHandler());
        chain.get(":path", new MiniProfilerResourceHandler());
    }
}
