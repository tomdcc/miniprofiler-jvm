/*
 * Copyright 2013 the original author or authors.
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

package io.jdev.miniprofiler.internal;

import io.jdev.miniprofiler.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class AutoClosableTest {

    @Test
    public void testAutoClosing() {
        String name = "My Function";
        ProfilerImpl profiler = new ProfilerImpl(name, ProfileLevel.Verbose, new DefaultProfilerProvider());
        doStuffWithAutoClosables(profiler);

        Timing rootTiming = profiler.getRoot();
        Assert.assertEquals(name, rootTiming.getName());
        List<Timing> children = rootTiming.getChildren();
        Assert.assertEquals(2, children.size());
        Timing firstChild = children.get(0);
        Timing secondChild = children.get(1);
        Assert.assertEquals("fooService.whatever", firstChild.getName());
        Assert.assertEquals("again", secondChild.getName());
        List<Timing> nestedChildren = firstChild.getChildren();
        Assert.assertEquals(1, nestedChildren.size());
        Timing nestedChild = nestedChildren.get(0);
        Assert.assertEquals("child", nestedChild.getName());
        Map<String, List<CustomTiming>> groupedNestedChildQueries = nestedChild.getCustomTimings();
        Assert.assertEquals(1, groupedNestedChildQueries.size());
        List<CustomTiming> nestedChildQueries = groupedNestedChildQueries.get("sql");
        Assert.assertEquals("select * from foo", nestedChildQueries.get(0).getCommandString());
        Map<String, List<CustomTiming>> secondGroupedNestedChildQueries = secondChild.getCustomTimings();
        Assert.assertEquals(1, secondGroupedNestedChildQueries.size());
        List<CustomTiming> secondChildQueries = secondGroupedNestedChildQueries.get("sql");
        Assert.assertEquals(2, secondChildQueries.size());
        Assert.assertEquals("select * from bar", secondChildQueries.get(0).getCommandString());
        Assert.assertEquals("select * from baz", secondChildQueries.get(1).getCommandString());
    }

    @Test
    public void testAutoClosingSafetyForNullProfiler() {
        Profiler profiler = new DefaultProfilerProvider().current();
        Assert.assertTrue(profiler instanceof NullProfiler);
        try(Profiler p = profiler) {
            doStuffWithAutoClosables(p);
        }
    }

    private void doStuffWithAutoClosables(Profiler profiler) {
        try (Timing firstTiming = profiler.step("fooService.whatever")) {
            try (Timing childTiming = profiler.step("child")) {
                profiler.addCustomTiming("sql", "query", "select * from foo", 5L);
            }
        }
        try (Timing lastTiming = profiler.step("again")) {
            lastTiming.addCustomTiming("sql", "query", "select * from bar", 5L);
            try (CustomTiming ct = lastTiming.customTiming("sql", "query", "select * from baz")) {
                System.currentTimeMillis(); // just here so we don't have an empty block
            }
        }
        profiler.stop();
    }


}
