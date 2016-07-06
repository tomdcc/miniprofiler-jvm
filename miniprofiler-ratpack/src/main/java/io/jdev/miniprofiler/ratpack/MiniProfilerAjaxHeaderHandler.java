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
import ratpack.exec.Execution;
import ratpack.handling.Context;
import ratpack.handling.Handler;

/**
 * Handler which adds the miniprofiler id for the current request as a response header.
 *
 * <p>Used by the UI javascript code to display AJAX profiler info.</p>
 */
public class MiniProfilerAjaxHeaderHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        ctx.maybeGet(Profiler.class).ifPresent(profiler -> {
            Execution.current().add(ProfilerStoreOption.class, ProfilerStoreOption.STORE_RESULTS);
            ctx.getResponse().getHeaders().add("X-MiniProfiler-Ids", "[\"" + profiler.getId() + "\"]");
        });
        ctx.next();
    }

}
