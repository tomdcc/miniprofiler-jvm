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

package io.jdev.miniprofiler.storage;

import io.jdev.miniprofiler.ProfilerImpl;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import java.util.UUID;

/**
 * Profiler storage that stored profiler instances in an Ehcache.
 */
public class EhcacheStorage extends BaseStorage {

	private final Ehcache cache;

	public EhcacheStorage(Ehcache cache) {
		this.cache = cache;
	}

	@Override
	public void save(ProfilerImpl profiler) {
		cache.put(new Element(profiler.getId(), profiler));
	}

	@Override
	public ProfilerImpl load(UUID id) {
		Element profilerElement = cache.get(id);
		if(profilerElement == null || profilerElement.isExpired()) {
			return null;
		}
		return (ProfilerImpl) profilerElement.getObjectValue();
	}
}
