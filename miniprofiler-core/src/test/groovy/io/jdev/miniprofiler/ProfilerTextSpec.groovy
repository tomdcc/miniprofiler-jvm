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
