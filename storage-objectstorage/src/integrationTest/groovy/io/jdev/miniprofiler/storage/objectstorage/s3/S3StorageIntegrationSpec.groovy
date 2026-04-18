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

package io.jdev.miniprofiler.storage.objectstorage.s3

import io.jdev.miniprofiler.ProfileLevel
import io.jdev.miniprofiler.ProfilerProvider
import io.jdev.miniprofiler.internal.ProfilerImpl
import io.jdev.miniprofiler.storage.Storage
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant

class S3StorageIntegrationSpec extends Specification {

    static final String BUCKET = "test-bucket"
    static final int PORT = 9090

    @Shared GenericContainer<?> container
    @Shared S3Client s3Client
    @Shared S3Storage storage
    @Shared ProfilerProvider profilerProvider

    void setupSpec() {
        profilerProvider = Mock(ProfilerProvider)

        container = new GenericContainer<>("adobe/s3mock:latest")
            .withExposedPorts(PORT)
            .waitingFor(Wait.forListeningPort())
        container.start()

        String endpoint = "http://${container.host}:${container.getMappedPort(PORT)}"
        def config = new S3StorageConfig(BUCKET, null, "us-east-1", endpoint)
        s3Client = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .region(Region.of("us-east-1"))
            .endpointOverride(URI.create(endpoint))
            .forcePathStyle(true)
            .build()
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build())
        storage = new S3Storage(config, s3Client)
    }

    void cleanupSpec() {
        storage?.close()
        s3Client?.close()
        container?.stop()
    }

    void cleanup() {
        storage.clear()
    }

    void "save and load round-trip"() {
        given:
        def profiler = new ProfilerImpl("test", ProfileLevel.Info, profilerProvider)

        when:
        storage.save(profiler)
        def loaded = storage.load(profiler.id)

        then:
        loaded != null
        loaded.id == profiler.id
        loaded.name == profiler.name
    }

    void "load returns null for unknown id"() {
        expect:
        storage.load(UUID.randomUUID()) == null
    }

    void "list returns profilers in ascending order"() {
        given:
        def p1 = profilerStartedAt(1000L)
        def p2 = profilerStartedAt(2000L)
        def p3 = profilerStartedAt(3000L)
        [p1, p2, p3].each { storage.save(it) }

        when:
        def ids = storage.list(10, null, null, Storage.ListResultsOrder.Ascending)

        then:
        ids as List == [p1.id, p2.id, p3.id]
    }

    void "list returns profilers in descending order"() {
        given:
        def p1 = profilerStartedAt(1000L)
        def p2 = profilerStartedAt(2000L)
        def p3 = profilerStartedAt(3000L)
        [p1, p2, p3].each { storage.save(it) }

        when:
        def ids = storage.list(10, null, null, Storage.ListResultsOrder.Descending)

        then:
        ids as List == [p3.id, p2.id, p1.id]
    }

    void "list filters by date range"() {
        given:
        def p1 = profilerStartedAt(1000L)
        def p2 = profilerStartedAt(5000L)
        def p3 = profilerStartedAt(9000L)
        [p1, p2, p3].each { storage.save(it) }

        when:
        def ids = storage.list(10, new Date(2000L), new Date(8000L), Storage.ListResultsOrder.Ascending)

        then:
        ids as List == [p2.id]
    }

    void "list respects maxResults"() {
        given:
        (1..5).each { storage.save(profilerStartedAt(it * 1000L)) }

        when:
        def ids = storage.list(3, null, null, Storage.ListResultsOrder.Ascending)

        then:
        ids.size() == 3
    }

    void "setUnviewed marks session as unviewed"() {
        given:
        def id = UUID.randomUUID()

        when:
        storage.setUnviewed("alice", id)

        then:
        storage.getUnviewedIds("alice").contains(id)
    }

    void "setViewed removes from unviewed"() {
        given:
        def id = UUID.randomUUID()
        storage.setUnviewed("alice", id)

        when:
        storage.setViewed("alice", id)

        then:
        !storage.getUnviewedIds("alice").contains(id)
    }

    void "getUnviewedIds returns only unviewed for given user"() {
        given:
        def id1 = UUID.randomUUID()
        def id2 = UUID.randomUUID()
        storage.setUnviewed("alice", id1)
        storage.setUnviewed("bob", id2)

        expect:
        storage.getUnviewedIds("alice") as List == [id1]
        storage.getUnviewedIds("bob") as List == [id2]
    }

    void "clear removes all data"() {
        given:
        def p = profilerStartedAt(1000L)
        storage.save(p)
        storage.setUnviewed("alice", p.id)

        when:
        storage.clear()

        then:
        storage.load(p.id) == null
        storage.list(10, null, null, Storage.ListResultsOrder.Ascending).isEmpty()
        storage.getUnviewedIds("alice").isEmpty()
    }

    void "expireOlderThan removes old sessions"() {
        given:
        def old = profilerStartedAt(1000L)
        def recent = profilerStartedAt(3000L)
        storage.save(old)
        storage.save(recent)

        when:
        storage.expireOlderThan(Instant.ofEpochMilli(2000L))

        then:
        storage.load(old.id) == null
        storage.load(recent.id) != null
    }

    private ProfilerImpl profilerStartedAt(long startedMs) {
        ProfilerImpl p = new ProfilerImpl("test", ProfileLevel.Info, profilerProvider)
        def field = ProfilerImpl.getDeclaredField("started")
        field.accessible = true
        field.set(p, startedMs)
        p.stop()
        return p
    }
}
