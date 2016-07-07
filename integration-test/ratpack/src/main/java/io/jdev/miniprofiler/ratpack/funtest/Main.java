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

import io.jdev.miniprofiler.ratpack.*;
import ratpack.groovy.template.TextTemplateModule;
import ratpack.guice.Guice;
import ratpack.server.*;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    public static void main(String... args) throws Exception {
        RatpackServer.start(server -> server
                .serverConfig(ServerConfig.builder().baseDir(BaseDir.find()))
                .registry(Guice.registry(bindings -> {
                    bindings.module(TextTemplateModule.class);
                    bindings.module(MiniProfilerHikariModule.class, hikariConfig -> {
                        hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
                        hikariConfig.addDataSourceProperty("URL", "jdbc:h2:mem:miniprofiler;DB_CLOSE_DELAY=-1");
                    });
                    bindings.module(MiniProfilerModule.class);
                    bindings.add(new DataSetup());
                    bindings.bind(TestHandler.class);
                }))
                .handlers(chain -> chain
                        .prefix(MiniProfilerHandlerChain.DEFAULT_PREFIX, MiniProfilerHandlerChain.class)
                        .files(f -> f.dir("public"))
                        .prefix("page", c -> c
                            .all(MiniProfilerStartProfilingHandler.class)
                            .all(MiniProfilerAjaxHeaderHandler.class)
                            .get(TestHandler.class)
                        )
                )
        );
    }

    private static class DataSetup implements Service {
        @Override
        public void onStart(StartEvent event) throws Exception {
            DataSource ds = event.getRegistry().get(DataSource.class);
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                st.executeUpdate("create table people (id bigint identity, name varchar(255) not null)");
                st.executeUpdate("insert into people(name) values ('Tom')");
                st.executeUpdate("insert into people(name) values ('Demi')");
            } catch(SQLException sqle) {
                throw new RuntimeException("Unexpected error setting up data", sqle);
            }
        }
    }

}
