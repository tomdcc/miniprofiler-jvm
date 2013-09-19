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

package io.jdev.miniprofiler.intercept;

import io.jdev.miniprofiler.ProfilerProvider;
import io.jdev.miniprofiler.Timing;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Invocation handler for profiling method calls using anything that can
 * take an {@link InvocationHandler}.
 */
public class ProfilingInvocationHandler implements InvocationHandler {

	private final ProfilerProvider profilerProvider;
	private final Object target;

	/**
	 * Create a new handler with the given profiler provider and target object
	 * @param profilerProvider the profiler provider to use
	 * @param target the target to invoke methods on
	 */
	public ProfilingInvocationHandler(ProfilerProvider profilerProvider, Object target) {
		this.profilerProvider = profilerProvider;
		this.target = target;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Timing timing =	profilerProvider.getCurrentProfiler().step(method.getDeclaringClass().getSimpleName() + "." + method.getName());
		try {
			return method.invoke(target, args);
		} finally {
			timing.stop();
		}
	}

	/**
	 * Convenience method to create a profiling proxy for the given target object and interface.
	 *
	 * @param profilerProvider the profiler provider to use
	 * @param target the target object to invoke methods on
	 * @param interfaceClass the interface that the returned proxy will implement
	 * @param <T> the interface that the returned proxy will implement
	 * @return a JDK dynamic proxy implementing the given interface
	 * @see Proxy
	 */
	public static <T> T createProxy(ProfilerProvider profilerProvider, Object target, Class<T> interfaceClass) {
		Class<?>[] interfaces = new Class<?>[] { interfaceClass };
		@SuppressWarnings("unchecked")
		T result = (T) createProxy(profilerProvider, target, interfaces);
		return result;
	}

	/**
	 * Convenience method to create a profiling proxy for the given target object and interfaces.
	 *
	 * @param profilerProvider the profiler provider to use
	 * @param target the target object to invoke methods on
	 * @param interfaces the interfaces that the returned proxy will implement
	 * @return a JDK dynamic proxy implementing the given interface
	 * @see Proxy
	 */
	public static Object createProxy(ProfilerProvider profilerProvider, Object target, Class<?>... interfaces) {
		return Proxy.newProxyInstance(target.getClass().getClassLoader(), interfaces, new ProfilingInvocationHandler(profilerProvider, target));
	}
}
