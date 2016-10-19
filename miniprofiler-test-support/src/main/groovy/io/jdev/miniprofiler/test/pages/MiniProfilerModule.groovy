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

package io.jdev.miniprofiler.test.pages

import geb.Module

class MiniProfilerModule extends Module {
    static base = { $('.profiler-results') }
    static content = {
        results { $('.profiler-result')*.module(MiniProfilerResultModule) }
    }
}

class MiniProfilerResultModule extends Module {
    static content = {
        button { $('.profiler-button').module(MiniProfilerButtonModule) }
        popup { $('.profiler-popup').module(MiniProfilerPopupModule) }
        queriesPopup { $('.profiler-queries').module(MiniProfilerQueriesPopupModule) }
    }
}

class MiniProfilerButtonModule extends Module {
    static content = {
        time { $('.profiler-number')?.text()?.trim() }
    }
}

class MiniProfilerPopupModule extends Module {
    static content = {
        timings { $('.profiler-output .profiler-timings tbody tr')*.module(MiniProfilerTimingRowModule) }
        toggleChildTimingLink { $('.profiler-toggle-hidden-columns') }
    }
}

class MiniProfilerTimingRowModule extends Module {
    static content = {
        label { $('.profiler-label')?.text()?.trim() }
        indent { $('.profiler-label .profiler-indent').text().length() }
        durations(cache: true) { $('.profiler-duration') }
        duration { durations[0] }
        durationWithChildren { durations[1] }
        timeFromStart { durations[2] }
        queries(required: false) { durations[3].find('a') }
    }
}

class MiniProfilerQueriesPopupModule extends Module {
    static content = {
        queries {
            $('table tbody tr').collect {
                it.module(it.hasClass('profiler-gap-info') ? MiniProfilerGapModule : MiniProfilerQueryModule)
            }
        }
        toggleTrivialGapsLink { $('.profiler-toggle-trivial-gaps') }
    }
}

class MiniProfilerQueryModule extends Module {
    static content = {
        profilerInfo(cache: true) { $('td.profiler-info > div') }
        step { profilerInfo[0]?.text()?.trim() }
        timeFromStart { profilerInfo[1]?.text()?.trim() }
        duration { profilerInfo[3]?.text()?.trim() }
        query { $('.query code')?.text()?.trim() }
    }

}

class MiniProfilerGapModule extends Module {
    static content = {
        trivial { $().hasClass('profiler-trivial-gaps') }
    }
}
