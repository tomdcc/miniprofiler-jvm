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

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jdev.miniprofiler.sql.ProfilingDataSource;
import ratpack.guice.ConfigurableModule;

import javax.sql.DataSource;

public class MiniProfilerHikariModule extends ConfigurableModule<HikariConfig> {

    @Override
    protected void configure() {

    }

    // Annoyingly, we can't extend Ratpack's HikariModule and extend it as Guice now disallows
    // overriding @Provides methods. So we have to reimplement it here.

    @Provides
    @Singleton
    public DataSource dataSource(HikariConfig config) {
        return new ProfilingDataSource(new HikariDataSource(config));
    }

}
