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

package io.jdev.miniprofiler.json;

import io.jdev.miniprofiler.MiniProfiler;
import io.jdev.miniprofiler.NullProfiler;
import io.jdev.miniprofiler.Profiler;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes out a script tag in the format that the mini profiler front end
 * javascript expects.
 */
public class ScriptTagWriter {

	/**
	 * Writes out a script tag in the format that the mini profiler front end
	 * javascript expects.
	 *
	 * @param profiler profiler data
	 * @param path path to the script
	 * @return script html tag
	 */
	public String printScriptTag(Profiler profiler, String path, Map tagAttributes) {
		if (profiler == null || profiler == NullProfiler.INSTANCE) {
			return "";
		}

		UUID currentId = profiler.getId();
		List<UUID> ids = Collections.singletonList(currentId);

		String position = (String) tagAttributes.get("data-position");
		boolean showTrivial = (Boolean) tagAttributes.get("data-trivial");
		boolean showChildren = (Boolean) tagAttributes.get("data-children");
		int maxTracesToShow = (Integer) tagAttributes.get("data-max-traces");
		boolean showControls = (Boolean) tagAttributes.get("data-controls");
		boolean authorized = (Boolean) tagAttributes.get("data-authorized");
		String toggleShortcut = (String) tagAttributes.get("data-toggle-shortcut");
		boolean startHidden = (Boolean) tagAttributes.get("data-start-hidden");

		String version = MiniProfiler.getVersion();

		if (!path.endsWith("/")) {
			path = path + "/";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<script async type='text/javascript' id='mini-profiler'");
		sb.append(" src='").append(path).append("includes.js?version=").append(version).append("'");
		appendAttribute(sb, "src", path + "includes.js?version=" + version);
		appendAttribute(sb, "data-version", version);
		appendAttribute(sb, "data-path", path);
		appendAttribute(sb, "data-current-id", currentId);
		appendAttribute(sb, "data-ids", ids.toString());
		appendAttribute(sb, "data-position", position);
		appendAttribute(sb, "data-trivial", showTrivial);
		appendAttribute(sb, "data-children", showChildren);
		appendAttribute(sb, "data-max-traces", maxTracesToShow);
		appendAttribute(sb, "data-controls", showControls);
		appendAttribute(sb, "data-authorized", authorized);
		appendAttribute(sb, "data-toggle-shortcut", toggleShortcut);
		appendAttribute(sb, "data-start-hidden", startHidden);
		sb.append("></script>");

		return sb.toString();
	}

	private static void appendAttribute(StringBuilder sb, String attributeName, Object value) {
		sb.append(" ").append(attributeName).append("='").append(value).append("'");
	}
}
