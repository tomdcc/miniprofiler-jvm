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

package io.jdev.miniprofiler.ratpack;

import io.jdev.miniprofiler.sql.ProfilingDataSource;
import ratpack.h2.H2Module;

import javax.sql.DataSource;

/**
 * A module that extends Ratpack's <code>H2Module</code> but provides a <code>DataSource</code>
 * that is actually a {@link ProfilingDataSource} so that JDBC calls are profiled.
 */
public class MiniProfilerH2Module extends H2Module {

    public MiniProfilerH2Module() {
    }

    public MiniProfilerH2Module(String username, String password, String url) {
        super(username, password, url);
    }

    protected DataSource createDataSource() {
        return new ProfilingDataSource(super.createDataSource());
    }

}
