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
    static base = { $('.mp-results') }
    static content = {
        results { $('.mp-result')*.module(MiniProfilerResultModule) }
    }
}

class MiniProfilerResultModule extends Module {
    static content = {
        button { $('.mp-button').module(MiniProfilerButtonModule) }
        popup { $('.mp-popup').module(MiniProfilerPopupModule) }
        queriesPopup { $('.mp-queries').module(MiniProfilerQueriesPopupModule) }
    }
}

class MiniProfilerButtonModule extends Module {
    static content = {
        time { $('.mp-number')?.text()?.trim() }
    }
}

class MiniProfilerPopupModule extends Module {
    static content = {
        name { $('.mp-name') }
        timings { $('.mp-output .mp-timings tbody tr')*.module(MiniProfilerTimingRowModule) }
        toggleChildTimingLink { $('.mp-toggle-columns').first() }
    }
}

class MiniProfilerTimingRowModule extends Module {
    static content = {
        label { $('.mp-label')?.text()?.trim() }
//        indent { $('.profiler-label .profiler-indent').text().length() }
        durations(cache: true) { $('.mp-duration') }
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
                it.module(it.hasClass('mp-gap-info') ? MiniProfilerGapModule : MiniProfilerQueryModule)
            }
        }
        toggleTrivialGapsLink { $('.mp-toggle-trivial-gaps') }
    }
}

class MiniProfilerQueryModule extends Module {
    static content = {
        // for some reason geckdriver returns empty for text() on these, so we use a different way to get it
        profilerInfo(cache: true) { $('td:first-child') }
        type { profilerInfo.$('.mp-call-type')?.attr('innerText')?.trim() }
        step { profilerInfo.$('div').not('.mp-call-type').not('.mp-number')?.attr('innerText')?.trim() }
        duration { profilerInfo.$('.mp-number')?.attr('innerText')?.trim() }
        query { $('.query code')?.attr('innerText')?.trim() }
    }

}

class MiniProfilerGapModule extends Module {
    static content = {
        trivial { $().hasClass('mp-trivial-gap') }
    }
}
