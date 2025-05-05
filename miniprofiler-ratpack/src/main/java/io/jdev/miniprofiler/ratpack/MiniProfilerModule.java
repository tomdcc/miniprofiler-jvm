/*
 * Copyright 2015 the original author or authors.
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

package io.jdev.miniprofiler.ratpack;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.jdev.miniprofiler.MiniProfiler;
import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerUiConfig;
import ratpack.exec.ExecInitializer;
import ratpack.guice.ConfigurableModule;

/**
 * A Guice module to install a Ratpack compatible {@link ProfilerProvider} and an {@link ExecInitializer}
 * to make all executions profiled.
 *
 * <p>The created {@link ProfilerProvider} is also installed as the default in the {@link MiniProfiler}
 * class for compatibility with code that doesn't use dependency injection or Ratpack contexts.</p>
 *
 * <p>This does <em>not</em> install handlers to support the UI - you'll need to do that separately
 * in your handler chain configuration.</p>
 */
public class MiniProfilerModule extends ConfigurableModule<MiniProfilerModule.Config> {

    /**
     * Installs Ratpack / MiniProfiler support code.
     */
    @Override
    protected void configure() {
        if (bindDefaultProvider()) {
            bind(ProfilerProvider.class).toProvider(ProviderProvider.class).in(Singleton.class);
        }

        requestStaticInjection(MiniProfilerModule.class);

        bind(MiniProfilerAjaxHeaderHandler.class).in(Scopes.SINGLETON);
        bind(MiniProfilerHandlerChain.class).in(Scopes.SINGLETON);
        bind(MiniProfilerStartProfilingHandlers.class).in(Scopes.SINGLETON);
        bind(StoreMiniProfilerHandler.class).in(Scopes.SINGLETON);
        bind(DiscardMiniProfilerHandler.class).in(Scopes.SINGLETON);
        bind(MiniProfilerResultsHandler.class).in(Scopes.SINGLETON);
        bind(MiniProfilerResourceHandler.class).in(Scopes.SINGLETON);
    }

    /**
     * Return whether to explicitly bind the default profiler provider.
     *
     * <p>Subclasses can return false here to bind their own profiler provider, e.g. using a provides method.</p>
     *
     * @return True if the module should bind a default profiler, false otherwise.
     */
    protected boolean bindDefaultProvider() {
        return true;
    }

    @ProvidesIntoSet
    @Singleton
    public ExecInitializer initializer(ProfilerProvider provider, Config config) {
        return createInitializer(provider, config);
    }

    @Provides
    @Singleton
    public MiniProfilerStartProfilingHandler startProfilingHandler(ProfilerProvider provider) {
        return new MiniProfilerStartProfilingHandler(provider);
    }

    protected MiniProfilerExecInitializer createInitializer(ProfilerProvider provider, Config config) {
        return new MiniProfilerExecInitializer(provider, config.defaultProfilerStoreOption);
    }

    public static class Config {
        public ProfilerStoreOption defaultProfilerStoreOption = ProfilerStoreOption.STORE_RESULTS;
        public ProfilerUiConfig uiConfig = ProfilerUiConfig.create();
    }

    @Inject
    private static void setStaticProfilerProvider(ProfilerProvider provider) {
        MiniProfiler.setProfilerProvider(provider);
    }

    private static class ProviderProvider implements Provider<ProfilerProvider> {
        private final Config config;

        @Inject
        private ProviderProvider(Config config) {
            this.config = config;
        }


        @Override
        public ProfilerProvider get() {
            RatpackContextProfilerProvider provider = new RatpackContextProfilerProvider();
            provider.setUiConfig(config.uiConfig);
            return provider;
        }
    }
}
