/*
 * Copyright 2016 the original author or authors.
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

import io.jdev.miniprofiler.Profiler;
import io.jdev.miniprofiler.Timing;
import ratpack.exec.*;
import ratpack.func.Action;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class containing utility methods for timing promises.
 */
public class MiniProfilerRatpackUtil {

    /**
     * Add a profiling step for the promise, starting with when the promise is first subscribed-to and finishing
     * when the promise supplies results.
     *
     * @param profiler the profiler to use
     * @param name the name of the profiling step
     * @param promise the promise to profile
     * @param <T> the type of the promise
     * @return a promise that has profiling instrumentation
     */
    public static <T> Promise<T> profile(Profiler profiler, String name, Promise<T> promise) {
        return promise.transform(up -> down -> {
            Timing step = profiler.step(name);
            up.connect(new ProfiledDownstream<>(down, step));
        });
    }

    /**
     * Add a profiling step for the promise, starting with the current time and finishing
     * when the promise supplies results.
     *
     * @param profiler the profiler to use
     * @param name the name of the profiling step
     * @param promise the promise to profile
     * @param <T> the type of the promise
     * @return a promise that has profiling instrumentation
     */
    public static <T> Promise<T> profileFromNow(Profiler profiler, String name, Promise<T> promise) {
        final Timing t = profiler.step(name);
        return promise.wiretap(r -> t.stop());
    }

    /**
     * Add a child profiler of the current execution's profiler to a forked execution.
     *
     * <p>
     *     Timing steps added to the child profiler will appear in the profiling report as children
     *     of the current profiling step, but the timings will not affect timing calculations as
     *     the work happens in parallel.
     * </p>
     *
     * @param execSpec the spec for the forked execution
     * @param forkedExecutionName the name to give the child
     * @param <T> The ExecSpec type (e.g. ExecStarter)
     * @return the passed-in spec
     */
    public static <T extends ExecSpec> T forkChildProfiler(T execSpec, String forkedExecutionName) {
        return forkChildProfiler(execSpec, forkedExecutionName, null);
    }

    /**
     * Add a child profiler of the current execution's profiler to a forked execution.
     *
     * <p>
     *     Timing steps added to the child profiler will appear in the profiling report as children
     *     of the current profiling step, but the timings will not affect timing calculations as
     *     the work happens in parallel.
     * </p>
     *
     * <p>
     *     This version accepts an onStart action to compose in, as the mechanism used to attach
     *     the child profiler to the new execution is via {@link ExecSpec#onStart(Action)}, but
     *     multiple calls to that method are not additive.
     * </p>
     *
     * @param execSpec the spec for the forked execution
     * @param forkedExecutionName the name to give the child
     * @param onStart an onStart action to execute after attaching the child profiler
     * @param <T> The ExecSpec type (e.g. ExecStarter)
     * @return the passed-in spec
     */
    public static <T extends ExecSpec> T forkChildProfiler(T execSpec, String forkedExecutionName, Action<? super Execution> onStart) {
        Optional<Execution> maybeParentExecution = Execution.currentOpt();
        AtomicBoolean setOnStart = new AtomicBoolean();
        maybeParentExecution.ifPresent(parentExecution -> {
            parentExecution.maybeGet(Profiler.class).ifPresent(profiler -> {
                setOnStart.set(true);
                execSpec.onStart(forkedExecution -> {
                    forkedExecution.add(Profiler.class, profiler.addChild(forkedExecutionName));
                    if (onStart != null) {
                        onStart.execute(forkedExecution);
                    }
                });
            });
        });
        if (onStart != null && !setOnStart.get()) {
            execSpec.onStart(onStart);
        }
        return execSpec;
    }

    private static class ProfiledDownstream<T> implements Downstream<T> {
        private final Downstream<T> down;
        private final Timing step;

        private ProfiledDownstream(Downstream<T> down, Timing step) {
            this.down = down;
            this.step = step;
        }

        @Override
        public void success(T value) {
            step.stop();
            down.success(value);
        }

        @Override
        public void error(Throwable throwable) {
            step.stop();
            down.error(throwable);
        }

        @Override
        public void complete() {
            step.stop();
            down.complete();
        }
    }
}
