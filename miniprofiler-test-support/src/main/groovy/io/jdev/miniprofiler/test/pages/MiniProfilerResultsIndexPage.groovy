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

package io.jdev.miniprofiler.test.pages

import geb.Page

class MiniProfilerResultsIndexPage extends Page {

    // Note that is deployed somewhere else this will need to be provided to
    // any go() calls.
    static url = "miniprofiler/results-index"

    static at = { title == "List of profiling sessions" }

    static content = {
        resultsTable(wait: true) { $('table.mp-results-index') }
        resultRows(wait: true) { $('table.mp-results-index tbody tr') }
    }
}
