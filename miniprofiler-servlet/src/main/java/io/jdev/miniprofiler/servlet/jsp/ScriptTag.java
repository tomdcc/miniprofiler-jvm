/*
 * Copyright 2018 the original author or authors.
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

package io.jdev.miniprofiler.servlet.jsp;

import io.jdev.miniprofiler.MiniProfiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ScriptTagWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

/**
 * JSP tag that writes out a HTML script tag to load MiniProfiler resources.
 */
@SuppressWarnings("WeakerAccess")
public class ScriptTag extends TagSupport {

    private ProfilerProvider profilerProvider;
    private ScriptTagWriter scriptTagWriter;

    public ScriptTag() {
        setProfilerProvider(MiniProfiler.getProfilerProvider());
    }

    // for testing
    ScriptTag(ProfilerProvider profilerProvider, ScriptTagWriter scriptTagWriter) {
        this.profilerProvider = profilerProvider;
        this.scriptTagWriter = scriptTagWriter;
    }

    @Override
    public int doEndTag() throws JspException {
        String contextPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath();
        try {
            pageContext.getOut().write(getContent(contextPath));
        } catch (IOException e) {
            throw new JspException(e);
        }
        return EVAL_PAGE;
    }

    // for testing
    String getContent(String contextPath) {
        return scriptTagWriter.printScriptTag(contextPath + profilerProvider.getUiConfig().getPath());
    }

    public void setProfilerProvider(ProfilerProvider profilerProvider) {
        this.profilerProvider = profilerProvider;
        this.scriptTagWriter = new ScriptTagWriter(profilerProvider);
    }

}
