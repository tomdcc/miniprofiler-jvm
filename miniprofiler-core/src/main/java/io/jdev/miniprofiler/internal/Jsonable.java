/*
 * Copyright 2013 the original author or authors.
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

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;

/**
 * Internal interface used by JSON serialization classes.
 */
interface Jsonable extends JSONAware, JSONStreamAware {
    JSONObject toJson();

    @Override
    default String toJSONString() {
        return toJson().toJSONString();
    }

    @Override
    default void writeJSONString(Writer out) throws IOException {
        toJson().writeJSONString(out);
    }
}
