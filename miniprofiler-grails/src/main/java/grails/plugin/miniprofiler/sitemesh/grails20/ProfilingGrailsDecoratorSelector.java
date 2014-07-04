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

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.Decorator;
import com.opensymphony.sitemesh.DecoratorSelector;
import com.opensymphony.sitemesh.SiteMeshContext;
import io.jdev.miniprofiler.Profiler;

public class ProfilingGrailsDecoratorSelector implements DecoratorSelector {

    private DecoratorSelector wrapped;
    private Profiler profiler;

    public ProfilingGrailsDecoratorSelector(DecoratorSelector wrapped, Profiler profiler) {
        this.wrapped = wrapped;
        this.profiler = profiler;
    }

    public Decorator selectDecorator(Content content, SiteMeshContext siteMeshContext) {
        return new ProfilingDecorator(wrapped.selectDecorator(content, siteMeshContext), profiler);
    }
}
