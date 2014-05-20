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

package io.jdev.miniprofiler;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class AutoClosableTest {

	@Test
	public void testAutoClosing() {
		String name = "My Function";
		ProfilerImpl profiler =  new ProfilerImpl(name, ProfileLevel.Verbose, new DefaultProfilerProvider());

		// add some stuff
		try (Timing firstTiming = profiler.step("fooService.whatever")) {
			try (Timing childTiming = profiler.step("child")) {
				profiler.addCustomTiming("sql", "query", "select * from foo", 5L);
			}
		}
		try (Timing lastTiming = profiler.step("again")) {
			profiler.addCustomTiming("sql", "query", "select * from bar", 5L);
		}

		profiler.stop();

		TimingImpl rootTiming = (TimingImpl) profiler.getRoot();
		Assert.assertEquals(name, rootTiming.getName());
		List<TimingImpl> children = rootTiming.getChildren();
		Assert.assertEquals(2, children.size());
		TimingImpl firstChild = children.get(0);
		TimingImpl secondChild = children.get(1);
		Assert.assertEquals("fooService.whatever", firstChild.getName());
		Assert.assertEquals("again", secondChild.getName());
		List<TimingImpl> nestedChildren = firstChild.getChildren();
		Assert.assertEquals(1, nestedChildren.size());
		TimingImpl nestedChild = nestedChildren.get(0);
		Assert.assertEquals("child", nestedChild.getName());
		Map<String,List<CustomTiming>> groupedNestedChildQueries = nestedChild.getCustomTimings();
		Assert.assertEquals(1, groupedNestedChildQueries.size());
		List<CustomTiming> nestedChildQueries = groupedNestedChildQueries.get("sql");
		Assert.assertEquals("select * from foo", nestedChildQueries.get(0).getCommandString());
		Map<String,List<CustomTiming>> secondGroupedNestedChildQueries = secondChild.getCustomTimings();
		Assert.assertEquals(1, secondGroupedNestedChildQueries.size());
		List<CustomTiming> secondChildQueries = secondGroupedNestedChildQueries.get("sql");
		Assert.assertEquals(1, secondChildQueries.size());
		Assert.assertEquals("select * from bar", secondChildQueries.get(0).getCommandString());
	}
}
