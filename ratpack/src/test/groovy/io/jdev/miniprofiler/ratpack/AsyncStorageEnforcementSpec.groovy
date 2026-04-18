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

package io.jdev.miniprofiler.ratpack

import groovy.io.FileType
import spock.lang.Specification

class AsyncStorageEnforcementSpec extends Specification {

    // Files that legitimately wrap sync Storage calls
    static final Set<String> ALLOWED_FILES = ['AsyncStorage.java'] as Set

    // Pre-existing violations that should be fixed separately
    // TODO: convert MiniProfilerResultsListHandler to use async storage for load() calls
    static final Set<String> KNOWN_EXCEPTIONS = [
        'MiniProfilerResultsListHandler.java',
    ] as Set

    // Patterns that indicate direct synchronous storage usage
    static final List<Map> SYNC_PATTERNS = [
        [pattern: ~/Ids\.buildIdsHeader\s*\([^)]*ProfilerProvider/, description: 'Ids.buildIdsHeader with ProfilerProvider calls sync storage internally'],
        [pattern: ~/\.getStorage\(\)\s*\.\s*(getUnviewedIds|setUnviewed|setViewed|save|load|list)\s*\(/, description: 'direct sync Storage method call via getStorage()'],
    ]

    void "ratpack production code must not call synchronous storage methods directly"() {
        given:
        def srcDir = new File(System.getProperty('user.dir'), 'src/main/java')
        def violations = []

        when:
        srcDir.eachFileRecurse(FileType.FILES) { file ->
            if (!file.name.endsWith('.java') || file.name in ALLOWED_FILES || file.name in KNOWN_EXCEPTIONS) {
                return
            }
            file.eachLine { line, lineNum ->
                SYNC_PATTERNS.each { entry ->
                    if (line =~ entry.pattern) {
                        violations << "${file.name}:${lineNum}: ${entry.description} -- ${line.trim()}"
                    }
                }
            }
        }

        then:
        violations.empty
    }
}
