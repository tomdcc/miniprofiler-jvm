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

package io.jdev.miniprofiler.storage.objectstorage.fs;

import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorage;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * File-system-backed {@link io.jdev.miniprofiler.storage.Storage} implementation.
 *
 * <p>Object keys are mapped directly to paths under a root directory. The same
 * key namespace ({@code profiler/}, {@code profiler-index/}, {@code unviewed/})
 * used by the cloud object stores is preserved, exercising the same logic in
 * {@link BaseObjectStorage}.</p>
 */
public class FileSystemStorage extends BaseObjectStorage {

    private final Path rootDir;

    /**
     * Creates storage rooted at the given directory.
     *
     * @param config the storage configuration
     */
    public FileSystemStorage(FileSystemStorageConfig config) {
        super(config, true);
        this.rootDir = config.getRootDir();
    }

    @Override
    protected void putObject(String key, byte[] content) {
        try {
            Path path = rootDir.resolve(key);
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write object: " + key, e);
        }
    }

    @Override
    protected byte[] getObject(String key) {
        try {
            return Files.readAllBytes(rootDir.resolve(key));
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read object: " + key, e);
        }
    }

    @Override
    protected void deleteObject(String key) {
        try {
            Files.deleteIfExists(rootDir.resolve(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete object: " + key, e);
        }
    }

    @Override
    protected Collection<String> listKeys(String keyPrefix) {
        Path prefixDir = rootDir.resolve(keyPrefix);
        List<String> result = new ArrayList<String>();
        if (!Files.isDirectory(prefixDir)) {
            return result;
        }
        try {
            Files.walkFileTree(prefixDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    result.add(rootDir.relativize(file).toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list keys with prefix: " + keyPrefix, e);
        }
        return result;
    }

    @Override
    protected void closeClient() {
        // nothing to close
    }
}
