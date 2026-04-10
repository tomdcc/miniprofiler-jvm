/*
 * Copyright 2026 the original author or authors.
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

import jakarta.persistence.*;

/** JPA entity representing a person in the WildFly 27 functional test application. */
@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /** Returns the person's ID. */
    public Long getId() {
        return id;
    }

    /** Sets the person's ID. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Returns the person's first name. */
    public String getFirstName() {
        return firstName;
    }

    /** Sets the person's first name. */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** Returns the person's last name. */
    public String getLastName() {
        return lastName;
    }

    /** Sets the person's last name. */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
