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

package io.jdev.miniprofiler

import io.jdev.miniprofiler.test.TestProfilerProvider
import spock.lang.Specification

class ProfilerTextSpec extends Specification {

    void "renders expected text"() {
        given:
        def profiler = new ProfilerImpl("name", "/foo", ProfileLevel.Info, new TestProfilerProvider())
        profiler.machineName = 'machine.name'
        def timing1 = profiler.step("timing1")
        def timing2 = profiler.step("timing2")
        timing2.addCustomTiming("sql", "query", "select * from foo", 123)
        timing2.stop()
        timing1.stop()
        profiler.stop()

        when:
        def lines = profiler.renderPlainText().readLines()

        then:
        lines.size() == 4
        lines[0] ==~ /machine.name at .*/
        lines[1] ==~ / \/foo = \d+ms/
        lines[2] ==~ /> timing1 = \d+ms/
        lines[3] ==~ />> timing2 = \d+ms \(sql = \d+ms in 1 cmd\)/
    }

}
