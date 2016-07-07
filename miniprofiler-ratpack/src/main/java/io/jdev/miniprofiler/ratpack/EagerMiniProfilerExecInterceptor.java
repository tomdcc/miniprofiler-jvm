/*
 * Copyright 2016 the original author or authors.
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

import io.jdev.miniprofiler.ProfilerProvider;
import ratpack.exec.Execution;

/**
 * A Ratpack execution interceptor that, as well as performing general cleanup at the end of an execution, will also
 * eagerly create a profiler and bind it to the execution at the start of execution.
 */
public class EagerMiniProfilerExecInterceptor extends MiniProfilerExecInterceptor {

    public EagerMiniProfilerExecInterceptor(ProfilerProvider provider, ProfilerStoreOption defaultProfilerStoreOption) {
        super(provider, defaultProfilerStoreOption);
    }

    public EagerMiniProfilerExecInterceptor(ProfilerProvider provider) {
        super(provider);
    }

    /**
     * This implementation returns true, ie always start a profiler.
     *
     * @param execution the execution whose segment is being intercepted
     * @param execType indicates whether this is a compute (e.g. request handling) segment or blocking segment
     * @return <code>true</code>
     */
    @Override
    protected boolean shouldCreateProfilerOnExecutionStart(Execution execution, ExecType execType) {
        return true;
    }

}
