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

package io.jdev.miniprofiler.jakarta.ee;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.ProfilerProviderLocator;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ProfilerProviderLocator} that looks up a {@link ProfilerProvider} from the
 * Jakarta CDI container via JNDI. Returns an empty {@link Optional} if the Jakarta CDI
 * API is not available or no {@link ProfilerProvider} bean is registered.
 */
public class CdiProfilerProviderLocator implements ProfilerProviderLocator {

    /** Default constructor. */
    public CdiProfilerProviderLocator() {
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ProfilerProvider> locate() {
        try {
            Object lookedUp = lookupBeanManagerFromJndi();
            if (!(lookedUp instanceof BeanManager)) {
                return Optional.empty();
            }
            BeanManager bm = (BeanManager) lookedUp;
            Set<Bean<?>> beans = bm.getBeans(ProfilerProvider.class);
            if (beans.isEmpty()) {
                return Optional.empty();
            }
            Bean<ProfilerProvider> bean = (Bean<ProfilerProvider>) bm.resolve(beans);
            CreationalContext<ProfilerProvider> ctx = bm.createCreationalContext(bean);
            Object reference = bm.getReference(bean, ProfilerProvider.class, ctx);
            if (!(reference instanceof ProfilerProvider)) {
                return Optional.empty();
            }
            return Optional.of((ProfilerProvider) reference);
        } catch (Exception | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }

    Object lookupBeanManagerFromJndi() throws Exception {
        return new InitialContext().lookup("java:comp/BeanManager");
    }
}
