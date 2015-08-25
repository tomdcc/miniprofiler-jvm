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

package io.jdev.miniprofiler.user;

/**
 * Interface to return the name of the current user.
 */
public interface UserProvider {

    /**
     * Returns the currently logged in user, if that is known.
     *
     * <p>This would normally be a username of some kind, but could potentially be
     * e.g. a job name for background tasks.</p>
     *
     * @return the current user, or null if it doesn't exist
     */
    String getUser();

}
