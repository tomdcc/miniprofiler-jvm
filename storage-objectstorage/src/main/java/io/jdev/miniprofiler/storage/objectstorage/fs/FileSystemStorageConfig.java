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

import io.jdev.miniprofiler.MiniProfilerConfig;
import io.jdev.miniprofiler.storage.objectstorage.BaseObjectStorageConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration for the file-system storage backend.
 *
 * <p>Properties are read via {@link MiniProfilerConfig}: system properties
 * (prefix {@code miniprofiler.}) take precedence over {@code miniprofiler.properties}
 * on the classpath.</p>
 *
 * <p>Supported keys: {@code storage.fs.directory}, {@code storage.fs.prefix}.</p>
 */
public class FileSystemStorageConfig extends BaseObjectStorageConfig {

    private final Path rootDir;

    /**
     * Creates a new instance with explicit values.
     *
     * @param rootDir   the root directory for storing objects; may be {@code null}
     * @param prefix    the optional key prefix; may be {@code null}
     */
    public FileSystemStorageConfig(Path rootDir, String prefix) {
        super(rootDir != null ? rootDir.toString() : null, prefix, null, null);
        this.rootDir = rootDir;
    }

    /**
     * Returns the root directory for storing objects.
     *
     * @return the root directory, or {@code null} if not configured
     */
    public Path getRootDir() {
        return rootDir;
    }

    /**
     * Creates a new {@link FileSystemStorageConfig} from system properties and
     * {@code miniprofiler.properties}, falling back to {@code null} for each unset property.
     *
     * @return a new config populated from the environment
     */
    public static FileSystemStorageConfig create() {
        return create(new MiniProfilerConfig());
    }

    static FileSystemStorageConfig create(Properties systemProps, Properties fileProps) {
        return create(new MiniProfilerConfig(systemProps, fileProps));
    }

    static FileSystemStorageConfig create(MiniProfilerConfig props) {
        String directory = props.getProperty("storage.fs.directory", (String) null);
        String prefix    = props.getProperty("storage.fs.prefix",   (String) null);
        Path rootDir = directory != null ? Paths.get(directory) : null;
        return new FileSystemStorageConfig(rootDir, prefix);
    }
}
