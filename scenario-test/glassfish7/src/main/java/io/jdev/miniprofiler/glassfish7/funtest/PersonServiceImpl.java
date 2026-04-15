/*
 * Copyright 2014-2026 the original author or authors.
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

package io.jdev.miniprofiler.glassfish7.funtest;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.Timing;
import io.jdev.miniprofiler.jakarta.ee.Profiled;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/** EJB implementation of {@link PersonService} for the GlassFish 7 functional test application. */
@Stateless
@Profiled
public class PersonServiceImpl implements PersonService {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private ProfilerProvider profilerProvider;

    @Override
    public List<Person> getAllPeople() {
        try (Timing timing = profilerProvider.current().step("First thing")) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        try (Timing timing = profilerProvider.current().step("Second thing")) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // ignore
            }
            return entityManager.createQuery("select p from Person p", Person.class).getResultList();
        }
    }

    @Override
    public Person createPerson(String firstName, String lastName) {
        Person p = new Person();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        entityManager.persist(p);
        return p;
    }

}
