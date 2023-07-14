/*
 * Copyright 2023 the original author or authors.
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

class MiniProfilerQueryModule extends Module {

    static content = {
        // for some reason geckodriver returns empty for text() on these, so we use a different way to get it
        profilerInfo(cache: true) { $('td:first-child') }
        type { profilerInfo.$('.mp-call-type')?.attr('innerText')?.trim() }
        step { profilerInfo.$('div').not('.mp-call-type').not('.mp-number')?.attr('innerText')?.trim() }
        duration { profilerInfo.$('.mp-number')?.attr('innerText')?.trim() }
        query { $('.query code')?.attr('innerText')?.trim() }
    }

}
