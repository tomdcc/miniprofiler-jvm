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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResourceHelper {
	private static final String RESOURCE_BASE_PATH = "io/jdev/miniprofiler/ui/";
	private static final int BUFFER_SIZE = 1024;

	private final ClassLoader classLoader;

	public ResourceHelper() {
		this.classLoader = getClass().getClassLoader();
	}

	public Resource getResource(String requestBasePath, String uri) throws IOException {
		requestBasePath = requestBasePath.endsWith("/") ? requestBasePath : requestBasePath + "/";
		InputStream stream = classLoader.getResourceAsStream(convertRequestPathToResourcePath(requestBasePath, uri));
		if (stream == null) return null;
		byte[] bytes = readResource(stream);
		return new Resource(bytes, guessContentType(uri));
	}

	private String guessContentType(String uri) {
		if (uri.endsWith(".css")) {
			return "text/css";
		} else if (uri.endsWith("js")) {
			return "text/javascript";
		} else {
			return "text/html";
		}
	}

	private byte[] readResource(InputStream stream) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			int read;
			while ((read = stream.read(buffer)) != -1) {
				os.write(buffer, 0, read);
			}
			return os.toByteArray();
		} finally {
			os.close();
			stream.close();
		}
	}

	public boolean uriMatches(String requestBasePath, String uri) {
		return uri.startsWith(requestBasePath);
	}

	public String convertRequestPathToResourcePath(String requestBasePath, String uri) {
		return RESOURCE_BASE_PATH + stripBasePath(requestBasePath, uri);
	}

	public String stripBasePath(String requestBasePath, String uri) {
		if (!uriMatches(requestBasePath, uri)) {
			throw new IllegalArgumentException("URI " + uri + " does not match request base path " + requestBasePath);
		}
		return uri.substring(requestBasePath.length());
	}

	public static class Resource {
		private final byte[] content;
		private final String contentType;

		public Resource(byte[] content, String contentType) {
			this.content = content;
			this.contentType = contentType;
		}

		public byte[] getContent() {
			return content;
		}

		public int getContentLength() {
			return content.length;
		}

		public String getContentType() {
			return contentType;
		}

	}
}
