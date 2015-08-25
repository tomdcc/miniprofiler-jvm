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

package io.jdev.miniprofiler.servlettest;

import io.jdev.miniprofiler.sql.ProfilingDataSource;
import org.h2.jdbcx.JdbcDataSource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Startup implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        DataSource ds = setupDataSource();
        setupData(ds);

        // since we want to profile stuff, do it
        ProfilingDataSource profilingDataSource = new ProfilingDataSource(ds);
        servletContextEvent.getServletContext().setAttribute("dataSource", profilingDataSource);
    }

    private DataSource setupDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:miniprofiler;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("sa");
        return ds;
    }

    private void setupData(DataSource ds) {
        try (Connection con = ds.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("create table people (id bigint identity, name varchar(255) not null)");
            st.executeUpdate("insert into people(name) values ('Tom')");
            st.executeUpdate("insert into people(name) values ('Demi')");
        } catch (SQLException sqle) {
            throw new RuntimeException("Unexpected error setting up data", sqle);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
