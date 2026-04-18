/*
 * Copyright 2017 the original author or authors.
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

package io.jdev.miniprofiler.internal;

import io.jdev.miniprofiler.CustomTiming;

class NullCustomTiming implements CustomTiming {

    static final NullCustomTiming INSTANCE = new NullCustomTiming();

    private NullCustomTiming() {
    }

    @Override
    public String getExecuteType() {
        return "";
    }

    @Override
    public String getCommandString() {
        return "";
    }

    @Override
    public long getStartMilliseconds() {
        return 0;
    }

    @Override
    public Long getDurationMilliseconds() {
        return null;
    }

    @Override
    public void stop() {
    }

    @Override
    public void close() {
    }
}
