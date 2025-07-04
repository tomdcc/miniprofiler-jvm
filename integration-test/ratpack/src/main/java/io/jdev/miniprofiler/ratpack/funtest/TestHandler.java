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
import com.google.inject.Inject;
import io.jdev.miniprofiler.ProfilerProvider;
import ratpack.exec.Blocking;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.jackson.Jackson;

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
    private final DataSource dataSource;

    @Inject
    public TestHandler(ProfilerProvider profilerProvider, DataSource dataSource) {
        this.profilerProvider = profilerProvider;
        this.dataSource = dataSource;
    }

    @Override
    public void handle(Context ctx) {
        profilerProvider.current().step("TestHandler.handle", () -> {
            Blocking.get(this::getData).then(data -> {
                ctx.byContent(spr -> {
                    spr.json(() -> ctx.render(Jackson.json(data)));
                    spr.html(() -> ctx.render(groovyTemplate(ImmutableMap.of("people", data), "index.html")));
                });
            });
        });
    }

    private List<Map<String, String>> getData() throws Exception {
        Thread.sleep(100); // to give predictable results in func test
        return profilerProvider.current().step("TestHandler.getData", () -> {
            List<Map<String, String>> results = new ArrayList<>();
            try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
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
