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

package io.jdev.miniprofiler.servlet;

import io.jdev.miniprofiler.MiniProfiler;
import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.json.JsonUtil;
import io.jdev.miniprofiler.util.ResourceHelper;
import io.jdev.miniprofiler.storage.Storage;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.UUID;

public class ProfilingFilter implements Filter {

	protected ProfilerProvider profilerProvider;
	protected String profilerPath = "/miniprofiler/";
	protected ResourceHelper resourceHelper = new ResourceHelper();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
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

		Profiler profiler = startProfiling(request);
		try {
			// add header, this is mostly for ajax
			UUID id = profiler.getId();
			if (id != null) {
				response.addHeader("X-MiniProfiler-Ids", "[\"" + id.toString() + "\"]");
			}
			filterChain.doFilter(servletRequest, servletResponse);
		} finally {
			profiler.stop();
		}
	}

	private void serveResource(HttpServletRequest request, HttpServletResponse response, String uri, String requestBasePath) throws IOException {
		if (resourceHelper.stripBasePath(requestBasePath, uri).equals("results")) {
			serveResults(request, response);
			return;
		}
		// serve up stuff
		ResourceHelper.Resource resource = resourceHelper.getResource(requestBasePath, uri);
		if (resource != null) {
			response.setContentLength(resource.getContentLength());
			response.setContentType(resource.getContentType());
			OutputStream os = response.getOutputStream();
			os.write(resource.getContent());
			os.close();
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void serveResults(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String id = request.getParameter("id");
		if (id == null || !id.matches("\\[[\\w\\-,]+\\]")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		id = id.substring(1, id.length() - 1);
		Storage storage = profilerProvider != null ? profilerProvider.getStorage() : MiniProfiler.getStorage();
		Profiler profiler = storage.load(UUID.fromString(id));
		if (profiler == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		response.setContentType("application/json");
		String json = JsonUtil.toJson(profiler);
		Writer writer = response.getWriter();
		writer.write(json);
		writer.close();
	}

	protected Profiler startProfiling(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (profilerProvider != null) {
			return profilerProvider.start(uri);
		} else {
			// use global one instead
			return MiniProfiler.start(uri);
		}
	}

	@Override
	public void destroy() {
	}

	protected boolean doProfilingForRequest(ServletRequest request) {
		return true;
	}

	/**
	 * Here so that DI frameworks can inject a profiler provider, rather than
	 * relying on {@link MiniProfiler#start(String)}.
	 *
	 * @param profilerProvider
	 */
	public void setProfilerProvider(ProfilerProvider profilerProvider) {
		this.profilerProvider = profilerProvider;
	}

}
