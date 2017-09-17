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

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.module.sitemesh.Page;
import io.jdev.miniprofiler.ProfilerProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

public class ProfilingDecoratorMapper implements DecoratorMapper {

    private final DecoratorMapper target;
    private final ProfilerProvider profilerProvider;

    public ProfilingDecoratorMapper(DecoratorMapper target, ProfilerProvider profilerProvider) {
        this.target = target;
        this.profilerProvider = profilerProvider;
    }

    @Override
    public void init(Config config, Properties properties, DecoratorMapper decoratorMapper) throws InstantiationException {
        target.init(config, properties, decoratorMapper);
    }

    @Override
    public Decorator getDecorator(HttpServletRequest httpServletRequest, Page page) {
        return wrapDecorator(target.getDecorator(httpServletRequest, page));
    }

    @Override
    public Decorator getNamedDecorator(HttpServletRequest httpServletRequest, String name) {
        return wrapDecorator(target.getNamedDecorator(httpServletRequest, name));
    }


    private Decorator wrapDecorator(Decorator decorator) {
        if (decorator == null) {
            return null;
        } else {
            return new ProfilingDecorator(decorator, profilerProvider.current());
        }
    }
}
