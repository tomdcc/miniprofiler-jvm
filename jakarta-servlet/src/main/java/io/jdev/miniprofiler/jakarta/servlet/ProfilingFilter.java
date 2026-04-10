/*
 * Copyright 2026 the original author or authors.
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

package io.jdev.miniprofiler.jakarta.servlet;

import io.jdev.miniprofiler.MiniProfiler;
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.StaticProfilerProvider;
import io.jdev.miniprofiler.server.Pages;
import io.jdev.miniprofiler.internal.ProfilerImpl;
import io.jdev.miniprofiler.server.IdParser;
import io.jdev.miniprofiler.sql.DriverUtil;
import io.jdev.miniprofiler.storage.Storage;
import io.jdev.miniprofiler.util.ResourceHelper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Filter to start and end profiling, and serve up results as JSON.
 *
 * <p>If a profiler provider is injected using
 * {@link #setProfilerProvider(io.jdev.miniprofiler.ProfilerProvider)}, then
 * it will be used to start new profiling sessions. Otherwise the filter will
 * use a {@link StaticProfilerProvider} which defers to {@link MiniProfiler},
 * and it will be up to any setup code to set the profiler provider statically using
 * {@link MiniProfiler#setProfilerProvider(io.jdev.miniprofiler.ProfilerProvider)}
 * if the default profiler provider isn't enough.</p>
 */
public class ProfilingFilter implements Filter {

    private static final String DEFAULT_PROFILER_PATH = "/miniprofiler/";
    private static final String PROFILER_PATH_PARAM = "path";
    private static final String ALLOWED_ORIGIN_PARAM = "allowed-origin";

    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String ID_PARAM = "id";

    /** The profiler provider used to start and retrieve profiling sessions. */
    protected ProfilerProvider profilerProvider = new StaticProfilerProvider();
    /** The URL path prefix for MiniProfiler resources, e.g. {@code "/miniprofiler"}. */
    protected String profilerPath = DEFAULT_PROFILER_PATH;
    /** Allowed {@code Origin} for CORS, or {@code null} to disallow cross-origin requests. */
    protected String allowedOrigin;
    /** Helper for serving MiniProfiler static resources. */
    protected ResourceHelper resourceHelper = new ResourceHelper();
    /** The servlet context this filter is registered in. */
    protected ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        String pathParam = filterConfig.getInitParameter(PROFILER_PATH_PARAM);
        if (pathParam != null) {
            if (!pathParam.startsWith("/")) {
                throw new IllegalArgumentException("Filter parameter " + PROFILER_PATH_PARAM + " must start with a /");
            }
            if (pathParam.equals("/")) {
                throw new IllegalArgumentException("Filter parameter " + PROFILER_PATH_PARAM + " must be a unique path more than just /, e.g. /miniprofiler");
            }
            if (!pathParam.endsWith("/")) {
                pathParam += "/";
            }
            profilerPath = pathParam;
        }
        allowedOrigin = filterConfig.getInitParameter(ALLOWED_ORIGIN_PARAM);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // handle serving up resources
        String uri = request.getRequestURI();
        String requestBasePath = request.getContextPath() + profilerPath;
        if (resourceHelper.uriMatches(requestBasePath, uri)) {
            serveResource(request, response, uri, requestBasePath);
            return;
        }

        if (!shouldProfileRequest(request)) {
            // just pass on
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            UUID id = null;
            String idParam = request.getParameter("x-miniprofiler-id");
            if (idParam != null && !idParam.isEmpty()) {
                try {
                    id = UUID.fromString(idParam);
                } catch (IllegalArgumentException e) {
                    // ignore, just generate it then
                }
            }
            Profiler profiler = startProfiling(id, request);
            try {
                // add header, this is mostly for ajax
                id = profiler.getId();
                if (id != null) {
                    response.addHeader("X-MiniProfiler-Ids", "[\"" + id.toString() + "\"]");
                }
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                profiler.stop();
            }
        }
    }

    private void serveResource(HttpServletRequest request, HttpServletResponse response, String uri, String requestBasePath) throws IOException {
        String stripped = resourceHelper.stripBasePath(requestBasePath, uri);
        if (stripped.equals("results")) {
            serveResults(request, response);
            return;
        }
        if (stripped.equals("results-index")) {
            serveResultsIndex(request, response);
            return;
        }
        if (stripped.equals("results-list")) {
            serveResultsList(request, response);
            return;
        }
        // serve up stuff
        ResourceHelper.Resource resource = resourceHelper.getResource(requestBasePath, uri);
        if (resource != null) {
            response.setContentLength(resource.getContentLength());
            response.setContentType(resource.getContentType());
            if (allowedOrigin != null) {
                response.addHeader("Access-Control-Allow-Origin", allowedOrigin);
            }
            OutputStream os = response.getOutputStream();
            os.write(resource.getContent());
            os.close();
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void serveResults(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean jsonRequest = Optional.ofNullable(request.getHeader(ACCEPT_HEADER))
            .map(header -> header.contains(CONTENT_TYPE_JSON)).orElse(false);

        UUID id = parseId(request, jsonRequest);
        if (id == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Profiler profiler = profilerProvider.getStorage().load(id);
        if (profiler == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (jsonRequest) {
            renderJson(profiler, response);
        } else {
            renderSingleResultHtml(profiler, request, response);
        }
    }

    private void serveResultsIndex(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_HTML);
        if (allowedOrigin != null) {
            response.addHeader("Access-Control-Allow-Origin", allowedOrigin);
        }
        try (Writer writer = response.getWriter()) {
            writer.write(Pages.renderResultListPage(profilerProvider, Optional.of(request.getContextPath() + profilerProvider.getUiConfig().getPath())));
        }
    }

    private void serveResultsList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Storage storage = profilerProvider.getStorage();
        Collection<UUID> ids = storage.list(100, null, null, Storage.ListResultsOrder.Descending);

        String lastIdParam = request.getParameter("last-id");
        if (lastIdParam != null && !lastIdParam.isEmpty()) {
            try {
                UUID lastId = UUID.fromString(lastIdParam);
                ProfilerImpl lastProfiler = storage.load(lastId);
                if (lastProfiler != null) {
                    long cutoff = lastProfiler.getStarted();
                    ids = ids.stream()
                        .filter(id -> {
                            ProfilerImpl p = storage.load(id);
                            return p != null && p.getStarted() > cutoff;
                        })
                        .collect(java.util.stream.Collectors.toList());
                }
            } catch (IllegalArgumentException ignored) {
                // ignore bad last-id
            }
        }

        response.setContentType(CONTENT_TYPE_JSON);
        if (allowedOrigin != null) {
            response.addHeader("Access-Control-Allow-Origin", allowedOrigin);
        }
        try (Writer writer = response.getWriter()) {
            writer.write(Pages.renderResultListJson(ids, storage));
        }
    }

    private static UUID parseId(HttpServletRequest request, boolean jsonRequest) {
        String body = null;
        if (jsonRequest) {
            try {
                body = getBody(request);
            } catch (IOException ignored) {
                // fall through to id param
            }
        }
        return IdParser.parseId(request.getHeader(ACCEPT_HEADER), body, request.getParameter(ID_PARAM));
    }

    private void renderJson(Profiler profiler, HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_JSON);
        if (allowedOrigin != null) {
            response.addHeader("Access-Control-Allow-Origin", allowedOrigin);
        }
        try (Writer writer = response.getWriter()) {
            writer.write(profiler.asUiJson());
        }
    }

    private void renderSingleResultHtml(Profiler profiler, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_HTML);
        if (allowedOrigin != null) {
            response.addHeader("Access-Control-Allow-Origin", allowedOrigin);
        }
        try (Writer writer = response.getWriter()) {
            writer.write(Pages.renderSingleResultPage(profiler, profilerProvider, Optional.of(request.getContextPath() + profilerProvider.getUiConfig().getPath())));
        }
    }

    private static String getBody(HttpServletRequest request) throws IOException {
        int contentLength = request.getContentLength();
        try (ServletInputStream is = request.getInputStream()) {
            byte[] buf = new byte[contentLength == -1 ? 2048 : contentLength];
            int offset = 0;
            int remaining = buf.length;
            int size;
            while (remaining > 0 && (size = is.read(buf, offset, remaining)) != -1) {
                offset += size;
                remaining -= size;
            }
            return offset == 0 ? null : new String(buf, 0, offset, StandardCharsets.UTF_8);
        }
    }

    /**
     * Starts a new profiling session for the given request.
     *
     * @param id      the profiler session ID
     * @param request the current HTTP request
     * @return the started {@link Profiler}
     */
    protected Profiler startProfiling(UUID id, HttpServletRequest request) {
        return profilerProvider.start(id, request.getRequestURI());
    }

    @Override
    public void destroy() {
        // in shutdown, let's deregister the profiling jdbc driver
        DriverUtil.deregisterDriverSpy();
    }

    /**
     * Called when the filter is determining whether to profile the request or not.
     * The default implementation won't profile requests which correspond to an actual
     * file in the web app, except for URIs ending in .jsp or /, as those may well be
     * dynamic anyway. Override to customise the default behaviour.
     *
     * @param request the request to profile (or not)
     * @return true if the request should be profiled, or false if not
     */
    protected boolean shouldProfileRequest(HttpServletRequest request) {
        try {
            String relativePath = request.getRequestURI().substring(request.getContextPath().length());
            if (servletContext.getResource(relativePath) == null) {
                // no static resource
                return true;
            }
            // guess if we're hitting a url ending in .jsp then it's not really
            // all that static, so do profiling
            // same for directory access
            return relativePath.endsWith(".jsp") || relativePath.equals("/");

        } catch (MalformedURLException e) {
            // this probably shouldn't happen, but if it does, they're probably not
            // requesting a static resource, which is what we're looking for here
            return true;
        }
    }

    /**
     * Here so that DI frameworks can inject a profiler provider, rather than
     * relying on {@link MiniProfiler#start(String)}.
     *
     * @param profilerProvider the current profiler provider
     */
    public void setProfilerProvider(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
    }

}
