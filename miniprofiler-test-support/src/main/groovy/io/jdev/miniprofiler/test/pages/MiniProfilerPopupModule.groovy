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
import org.openqa.selenium.By
import org.openqa.selenium.Keys

class MiniProfilerPopupModule extends Module {

    static content = {
        name { $('.mp-name') }
        timings { $('.mp-output .mp-timings tbody tr').moduleList(MiniProfilerTimingRowModule) }
        toggleChildTimingLink { $('.mp-toggle-columns').first() }
        shareLink { $('.mp-share-mp-results') }
    }

    void close() {
        if (displayed) {
            def body = $(By.xpath('/html/body'))
            body.firstElement().sendKeys(Keys.ESCAPE)
            waitFor { !body.find('.mp-popup').any { it.displayed } }
        }
    }

}
