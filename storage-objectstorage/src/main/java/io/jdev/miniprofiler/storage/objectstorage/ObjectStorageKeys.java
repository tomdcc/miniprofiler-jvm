/*
 * Copyright 2026 the original author or authors.
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

package io.jdev.miniprofiler.storage.objectstorage;

import java.util.UUID;

/**
 * Stateless helper that calculates object storage key names for profiler data.
 *
 * <p>All keys are relative to an optional prefix supplied at construction time.
 * Three key namespaces are used:</p>
 * <ul>
 *   <li>{@code {prefix}profiler/{uuid}} — the serialised profiler JSON</li>
 *   <li>{@code {prefix}profiler-index/{19digits}-{uuid}} — empty marker for listing/ordering</li>
 *   <li>{@code {prefix}unviewed/{user}/{uuid}} — empty marker for unviewed sessions</li>
 * </ul>
 */
public class ObjectStorageKeys {

    private final String prefix;

    /**
     * Creates a new instance with the given key prefix.
     *
     * @param prefix the prefix to prepend to all keys; {@code null} is treated as empty string
     */
    public ObjectStorageKeys(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    /**
     * Returns the key used to store the serialised profiler JSON.
     *
     * @param id the profiler session id
     * @return the object key
     */
    public String profilerKey(UUID id) {
        return prefix + "profiler/" + id.toString();
    }

    /**
     * Returns the index key for a profiler session. Index keys sort lexicographically
     * in chronological order because the timestamp is zero-padded to 19 digits.
     *
     * @param startedMillis the session start time in epoch milliseconds
     * @param id            the profiler session id
     * @return the index object key
     */
    public String indexKey(long startedMillis, UUID id) {
        return prefix + "profiler-index/" + String.format("%019d", startedMillis) + "-" + id.toString();
    }

    /**
     * Returns the prefix used when listing all profiler data keys.
     *
     * @return the profiler key prefix
     */
    public String profilerPrefix() {
        return prefix + "profiler/";
    }

    /**
     * Returns the prefix used when listing all index keys.
     *
     * @return the index key prefix
     */
    public String indexPrefix() {
        return prefix + "profiler-index/";
    }

    /**
     * Returns the prefix used when listing all unviewed markers (any user).
     *
     * @return the unviewed key prefix
     */
    public String allUnviewedPrefix() {
        return prefix + "unviewed/";
    }

    /**
     * Returns the key used to mark a profiler session as unviewed for a given user.
     *
     * @param user the user name
     * @param id   the profiler session id
     * @return the unviewed marker key
     */
    public String unviewedKey(String user, UUID id) {
        return prefix + "unviewed/" + user + "/" + id.toString();
    }

    /**
     * Returns the prefix used when listing unviewed markers for a given user.
     *
     * @param user the user name
     * @return the unviewed key prefix for the user
     */
    public String unviewedPrefix(String user) {
        return prefix + "unviewed/" + user + "/";
    }

    /**
     * Extracts the session UUID from an index key.
     *
     * <p>Index keys have the form {@code {prefix}profiler-index/{19digits}-{uuid}}.
     * This method finds the first {@code '-'} character at position 19 or later
     * (counting from after the prefix and {@code profiler-index/} segment) and
     * parses the UUID that follows it.</p>
     *
     * @param key the full index key
     * @return the extracted UUID, or {@code null} if the key is malformed
     */
    public UUID extractIdFromIndexKey(String key) {
        String indexPfx = indexPrefix();
        if (!key.startsWith(indexPfx)) {
            return null;
        }
        String afterPrefix = key.substring(indexPfx.length());
        // format is {19digits}-{uuid}; find the '-' separator at position >= 19
        int dashPos = afterPrefix.indexOf('-', 19);
        if (dashPos < 0) {
            return null;
        }
        try {
            return UUID.fromString(afterPrefix.substring(dashPos + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
