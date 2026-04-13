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

package io.jdev.miniprofiler.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class for loading internal resources.
 */
public class ResourceHelper {
    private static final String RESOURCE_BASE_PATH = "io/jdev/miniprofiler/ui/";
    private static final int BUFFER_SIZE = 1024;

    private final String resourceBasePath;
    private final ClassLoader classLoader;

    /** Creates a new instance using the default resource base path. */
    public ResourceHelper() {
        this(RESOURCE_BASE_PATH);
    }

    /**
     * Creates a new instance using the given resource base path.
     *
     * @param resourceBasePath the classpath prefix under which static resources are located
     */
    public ResourceHelper(String resourceBasePath) {
        resourceBasePath = resourceBasePath.endsWith("/") ? resourceBasePath : resourceBasePath + "/";
        this.resourceBasePath = resourceBasePath;
        this.classLoader = getClass().getClassLoader();
    }

    /**
     * Returns the resource at the given URI, resolved relative to the request base path, or null if not found.
     *
     * @param requestBasePath the base path prefix to strip from the URI
     * @param uri the request URI
     * @return the resource, or null if not found
     * @throws IOException if reading the resource fails
     */
    public Resource getResource(String requestBasePath, String uri) throws IOException {
        requestBasePath = requestBasePath.endsWith("/") ? requestBasePath : requestBasePath + "/";
        return getResource(stripBasePath(requestBasePath, uri));
    }

    /**
     * Returns the named resource, or null if not found.
     *
     * @param resourceName the resource path relative to the resource base
     * @return the resource, or null if not found
     * @throws IOException if reading the resource fails
     */
    public Resource getResource(String resourceName) throws IOException {
        InputStream stream = classLoader.getResourceAsStream(RESOURCE_BASE_PATH + resourceName);
        if (stream == null) {
            return null;
        }
        byte[] bytes = readResource(stream);
        return new Resource(bytes, guessContentType(resourceName));
    }

    /**
     * Returns the content of the named resource as a string, or null if not found.
     *
     * @param resource the resource path relative to the resource base path
     * @return the resource content as a string, or null if not found
     * @throws IOException if reading the resource fails
     */
    public String getResourceAsString(String resource) throws IOException {
        InputStream stream = classLoader.getResourceAsStream(resourceBasePath + resource);
        if (stream == null) {
            return null;
        }
        byte[] bytes = readResource(stream);
        return new String(bytes);
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

    /**
     * Returns true if the given URI starts with the given request base path.
     *
     * @param requestBasePath the base path to match against
     * @param uri the URI to check
     * @return true if the URI starts with the base path
     */
    public boolean uriMatches(String requestBasePath, String uri) {
        return uri.startsWith(requestBasePath);
    }

    /**
     * Strips the request base path prefix from the given URI and returns the remainder.
     *
     * @param requestBasePath the base path prefix to strip
     * @param uri the URI to strip the prefix from
     * @return the URI with the base path prefix removed
     */
    public String stripBasePath(String requestBasePath, String uri) {
        if (!uriMatches(requestBasePath, uri)) {
            throw new IllegalArgumentException("URI " + uri + " does not match request base path " + requestBasePath);
        }
        return uri.substring(requestBasePath.length());
    }

    /** A loaded resource with its raw content and content type. */
    public static class Resource {
        private final byte[] content;
        private final String contentType;

        /**
         * Creates a new resource with the given content and content type.
         *
         * @param content the raw bytes of the resource
         * @param contentType the MIME content type
         */
        public Resource(byte[] content, String contentType) {
            this.content = content;
            this.contentType = contentType;
        }

        /**
         * Returns the raw bytes of the resource content.
         *
         * @return the resource content bytes
         */
        public byte[] getContent() {
            return content;
        }

        /**
         * Returns the length in bytes of the resource content.
         *
         * @return the content length in bytes
         */
        public int getContentLength() {
            return content.length;
        }

        /**
         * Returns the MIME content type of the resource.
         *
         * @return the MIME content type
         */
        public String getContentType() {
            return contentType;
        }

    }
}
