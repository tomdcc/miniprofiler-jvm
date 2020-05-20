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

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public final class CargoTestExecutionListener implements TestExecutionListener {

    private CargoTestFixture cargoTestFixture;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        cargoTestFixture = CargoTestSetup.createFixtureFromSystemProps();
        cargoTestFixture.start();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        cargoTestFixture.stop();
    }

}
