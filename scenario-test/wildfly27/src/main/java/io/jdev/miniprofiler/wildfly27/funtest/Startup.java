/*
 * Copyright 2013-2026 the original author or authors.
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

package io.jdev.miniprofiler.wildfly27.funtest;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Populates initial test data on application startup for the WildFly 27 functional test application. */
@WebListener
public class Startup implements ServletContextListener {

    @EJB
    private PersonService personService;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        personService.createPerson("Alex", "Smith");
        personService.createPerson("Kim", "Jones");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
