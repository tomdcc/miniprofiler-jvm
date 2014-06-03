/*
 * Copyright 2014 the original author or authors.
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

package io.jdev.miniprofiler.glassfish4;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class PersonServiceImpl implements PersonService {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Person> getAllPeople() {
		return entityManager.createQuery("select p from Person p", Person.class).getResultList();
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
