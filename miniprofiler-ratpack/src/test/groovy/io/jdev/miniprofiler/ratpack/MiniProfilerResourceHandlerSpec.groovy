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

package io.jdev.miniprofiler.ratpack

import ratpack.func.Action
import ratpack.test.handling.RequestFixture
import spock.lang.Specification

class MiniProfilerResourceHandlerSpec extends Specification {

    void "handler returns response and correct content type"() {
        given: "resource"
        def resName = 'includes.js'
        def expectedBytes = Thread.currentThread().contextClassLoader
                .getResourceAsStream("io/jdev/miniprofiler/ui/$resName").bytes


        when: 'ask for resource'
        def result = RequestFixture.handle(new MiniProfilerResourceHandler(), { RequestFixture req ->
            req.pathBinding(path: resName)
        } as Action)

        then:
        result.sentResponse
        result.status.code == 200
        result.bodyBytes == expectedBytes
    }

    void "handler returns 404 for and correct content type"() {
        when: 'ask for non-existent resource'
        def result = RequestFixture.handle(new MiniProfilerResourceHandler(), { RequestFixture req ->
            req.pathBinding(path: "notthere")
        } as Action)

        then:
        result.sentResponse
        result.status.code == 404
    }
}
