/*
 * Copyright 2022-2026 the original author or authors.
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

package io.jdev.miniprofiler.server;

import io.jdev.miniprofiler.internal.ClientTiming;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.util.List;
import java.util.UUID;

/**
 * Represents a request for profiling results, parsed from a JSON payload.
 */
public final class ResultsRequest {

    /** The unique identifier of the requested profiling session. */
    public final UUID id;

    /** Client-side performance timings, or null if not provided. */
    public final List<ClientTiming> clientTimings;

    private ResultsRequest(UUID id, List<ClientTiming> clientTimings) {
        this.id = id;
        this.clientTimings = clientTimings;
    }

    /**
     * Returns the profiling session id.
     *
     * @return the session UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Parses a JSON request string into a {@code ResultsRequest}.
     *
     * @param request the JSON string containing an "Id" property
     * @return the parsed results request
     * @throws IllegalArgumentException if the JSON is invalid or missing a valid Id
     */
    public static ResultsRequest from(String request) {
        Object requestObj;
        try {
            requestObj = JSONValue.parseWithException(request);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Results request was not a valid JSON object", e);
        }

        if (!(requestObj instanceof JSONObject)) {
            throw new IllegalArgumentException("Results request was not a JSON object");
        }
        JSONObject reqJson = (JSONObject) requestObj;

        Object idObject = reqJson.get("Id");
        if (idObject == null) {
            throw new IllegalArgumentException("Results request did not contain an Id property");
        }

        if (!(idObject instanceof String)) {
            throw new IllegalArgumentException("Results request Id property was not a string");
        }
        String id = (String) idObject;

        // sometimes the UI sends through ids wrapped in square brackets
        if (id.startsWith("[") && id.endsWith("]")) {
            id = id.substring(1, id.length() - 1);
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Results request Id property was not a UUID", e);
        }

        List<ClientTiming> clientTimings = ClientTiming.listFromJson((JSONArray) reqJson.get("Performance"));
        return new ResultsRequest(uuid, clientTimings);
    }
}
