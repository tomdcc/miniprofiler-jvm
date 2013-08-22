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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Utility class used for JSON serialization.
 */
public class JsonUtil {
	public static List<Map<String, Object>> mapList(List<? extends Jsonable> srcList) {
		if (srcList == null) return null;

		List<Map<String, Object>> dest = new ArrayList<Map<String, Object>>(srcList.size());
		for (Jsonable srcObject : srcList) {
			dest.add(srcObject != null ? srcObject.toMap() : null);
		}
		return dest;
	}

	public static String toJson(Jsonable src) {
		StringBuilder buf = new StringBuilder();
		mapToJsonBuffer(buf, src.toMap());
		return buf.toString();
	}

	private static void objectToJsonBuffer(StringBuilder buf, Object obj) {
		if (obj == null) {
			buf.append("null");
		} else if (obj instanceof Jsonable) {
			mapToJsonBuffer(buf, ((Jsonable) obj).toMap()); //
		} else if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) obj;
			mapToJsonBuffer(buf, map); //
		} else if (obj instanceof Collection) {
			collectionToJsonBuffer(buf, (Collection) obj);
		} else if (obj instanceof Number || obj instanceof Boolean) {
			buf.append(obj.toString());
		} else {
			// treat it like a string, regardless
			stringToJsonBuffer(buf, obj.toString());
		}
	}

	private static void stringToJsonBuffer(StringBuilder buf, String str) {
		if (str == null) {
			buf.append("null");
		} else {
			str = str.replaceAll("\\\\", "\\\\");
			str = str.replaceAll("\"", "\\\"");
			str = str.replaceAll("\r\n", "\\\\n");
			str = str.replaceAll("\n", "\\\\n");
			buf.append('"').append(str).append('"');
		}
	}

	private static void collectionToJsonBuffer(StringBuilder buf, Collection<?> src) {
		buf.append("[");
		boolean first = true;
		for (Object val : src) {
			if (first) {
				first = false;
			} else {
				buf.append(',');
			}
			objectToJsonBuffer(buf, val);
		}
		buf.append("]");
	}

	private static void mapToJsonBuffer(StringBuilder buf, Map<String, Object> src) {
		buf.append("{");
		boolean first = true;
		for (Map.Entry<String, Object> entry : src.entrySet()) {
			if (first) {
				first = false;
			} else {
				buf.append(',');
			}
			stringToJsonBuffer(buf, entry.getKey());
			buf.append(':');
			objectToJsonBuffer(buf, entry.getValue());
		}
		buf.append("}");
	}
}
