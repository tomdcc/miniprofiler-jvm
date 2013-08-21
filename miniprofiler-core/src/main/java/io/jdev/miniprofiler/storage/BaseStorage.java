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

package io.jdev.miniprofiler.storage;

import java.util.*;

public abstract class BaseStorage implements Storage {

    /**
     * Default implementation. Returns no results
     */
    public Collection<UUID> list(int maxResults, Date start, Date finish, ListResultsOrder orderBy) {
        return Collections.emptyList();
    }

    /**
     * Default implementation. Does nothing.
     */
    public void setUnviewed(String user, UUID id) {
    }

    /**
     * Default implementation. Does nothing.
     */
    public void setViewed(String user, UUID id) {
    }

    /**
     * Default implementation. Returns no results
     */
    public Collection<UUID> getUnviewedIds(String user) {
        return Collections.emptyList();
    }

}
