/*
 * Copyright 2014 the original author or authors.
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

package grails.plugin.miniprofiler.sitemesh.grails20;

import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import io.jdev.miniprofiler.ProfilerProvider;
import org.codehaus.groovy.grails.web.sitemesh.GrailsPageFilter;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.FilterConfig;
import java.lang.reflect.Field;

public class ProfilingGrailsPageFilter extends GrailsPageFilter {

    private ProfilerProvider profilerProvider;

    @Override
    public void init(FilterConfig fc) {
        super.init(fc);
        profilerProvider = WebApplicationContextUtils.getRequiredWebApplicationContext(fc.getServletContext()).getBean("profilerProvider", ProfilerProvider.class);

        Field field = null;
        try {
            field = GrailsPageFilter.class.getDeclaredField("decoratorMapper");
            field.setAccessible(true);
            DecoratorMapper decoratorMapper = (DecoratorMapper) field.get(this);
            field.set(this, new ProfilingDecoratorMapper(decoratorMapper, profilerProvider));
        } catch (NoSuchFieldException e) {
            // different grails version which doesn't have that field?
        } catch (IllegalAccessException e) {
            // just won't work, we're in a security manager
        }
    }

    @Override
    protected DecoratorSelector initDecoratorSelector(SiteMeshWebAppContext webAppContext) {
        DecoratorSelector selector = super.initDecoratorSelector(webAppContext);
        if (profilerProvider.hasCurrent()) {
            return new ProfilingGrailsDecoratorSelector(selector, profilerProvider.current());
        } else {
            return selector;
        }
    }
}
