/*
 * Copyright 2020 the original author or authors.
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

package io.jdev.miniprofiler.testlib.cargo;

import org.junit.platform.commons.util.ExceptionUtils;

import java.net.URL;

class CargoTestSetup {

    private static final String SYSTEM_PROP_PREFIX = "testContext.cargoTest.";

    static CargoTestFixture createFixtureFromSystemProps() {
        try {
            return new CargoTestFixture(
                    requiredSystemProp("containerId"),
                    new URL(requiredSystemProp("downloadUrl")),
                    Integer.parseInt(requiredSystemProp("port")),
                    requiredSystemProp("warPath"),
                    requiredSystemProp("contextPath")
            );
        } catch (Exception e) {
            throw ExceptionUtils.throwAsUncheckedException(e);
        }
    }

    private static String requiredSystemProp(String name) {
        String fullPropName = SYSTEM_PROP_PREFIX + name;
        String val = System.getProperty(fullPropName);
        if (val == null) {
            throw new IllegalArgumentException("System property " + fullPropName  +" required");
        }
        return val;
    }
}
