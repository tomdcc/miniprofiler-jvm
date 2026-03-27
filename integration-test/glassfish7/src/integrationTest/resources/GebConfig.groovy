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

// Use the URL provided by the container manager (set as a system property),
// falling back to localhost for running against a manually started server.
baseUrl = System.getProperty("geb.build.baseUrl") ?: 'http://127.0.0.1:8080/'

if(!System.getProperty("geb.build.reportsDir")) {
	// probably running in IDE
	reportsDir = 'build/reports/geb'
}
