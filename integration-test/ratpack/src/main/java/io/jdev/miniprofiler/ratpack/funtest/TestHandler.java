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

package io.jdev.miniprofiler.ratpack.funtest;

import com.google.common.collect.ImmutableMap;
import io.jdev.miniprofiler.ProfilerProvider;
import ratpack.exec.Blocking;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.jackson.Jackson;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ratpack.groovy.Groovy.groovyTemplate;

public class TestHandler implements Handler {

    private final ProfilerProvider profilerProvider;

    @Inject
    public TestHandler(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        profilerProvider.getCurrentProfiler().step("TestHandler.handle", () -> {
            Blocking.get(() -> getData(ctx)).then(data -> {
                ctx.byContent(spr -> {
                    spr.json(() -> ctx.render(Jackson.json(data)));
                    spr.html(() -> ctx.render(groovyTemplate(ImmutableMap.of("people", data), "index.html")));
                });
            });
        });
    }

    private List<Map<String, String>> getData(Context ctx) throws Exception {
        return profilerProvider.getCurrentProfiler().step("TestHandler.getData", () -> {
            List<Map<String, String>> results = new ArrayList<>();
            try (Connection con = ctx.get(DataSource.class).getConnection(); Statement st = con.createStatement()) {
                try (ResultSet rs = st.executeQuery("select * from people")) {
                    while (rs.next()) {
                        Map<String, String> row = new HashMap<String, String>();
                        row.put("id", rs.getString("id"));
                        row.put("name", rs.getString("name"));
                        results.add(row);
                    }
                    return results;
                }
            }
        });
    }
}
