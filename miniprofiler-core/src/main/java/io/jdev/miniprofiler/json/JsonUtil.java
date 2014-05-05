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

import java.util.*;

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
			quote(str, buf);
		}
	}
	/**
	 * Produce a string in double quotes with backslash sequences in all the
	 * right places. A backslash will be inserted within </, allowing JSON
	 * text to be delivered in HTML. In JSON text, a string cannot contain a
	 * control character or an unescaped quote or backslash.
	 *
	 * This code copied from The Jettison project JSONObject class, also under
	 * the Apcache 2.0 license.
	 *
	 * @param string A String
	 * @return  A String correctly formatted for insertion in a JSON text.
	 */
	public static void quote(String string, StringBuilder sb) {
		char         c = 0;
		int          i;
		int          len = string.length();
		String       t;

		sb.append('"');
		for (i = 0; i < len; i += 1) {
			c = string.charAt(i);
			switch (c) {
				case '\\':
				case '"':
					sb.append('\\');
					sb.append(c);
					break;
				case '/':
					sb.append('\\');
					sb.append(c);
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\r':
					sb.append("\\r");
					break;
				default:
					if (c < ' ') {
						t = "000" + Integer.toHexString(c);
						sb.append("\\u" + t.substring(t.length() - 4));
					} else {
						sb.append(c);
					}
			}
		}
		sb.append('"');
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

	private static void mapToJsonBuffer(StringBuilder buf, Map<String, ?> src) {
		buf.append("{");
		boolean first = true;
		for (Map.Entry<String, ?> entry : src.entrySet()) {
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
