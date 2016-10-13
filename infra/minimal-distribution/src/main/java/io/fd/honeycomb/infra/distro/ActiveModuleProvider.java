/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.infra.distro;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides list of active modules for distribution
 */
public class ActiveModuleProvider {

    public static final String STANDARD_MODULES_RELATIVE_PATH = "../modules/";
    private static final Logger LOG = LoggerFactory.getLogger(ActiveModuleProvider.class);

    /**
     * Provide unique set of active modules filtered from provided resources
     */
    public static Set<Module> loadActiveModules(@Nonnull final List<String> moduleNames) {
        final ClassLoader classLoader = ActiveModuleProvider.class.getClassLoader();
        LOG.info("Reading active modules configuration for distribution");

        // process resources to resource modules
        return moduleNames.stream()
                .map(String::trim)
                .filter(trimmedLine -> trimmedLine.length() != 0)
                // filter out commented lines
                .filter(nonEmptyLine -> !nonEmptyLine.startsWith("//"))
                // filter duplicates
                .distinct()
                .map(validLine -> nameToClass(validLine, classLoader))
                // filters out classes that are not modules
                .filter(ActiveModuleProvider::filterNonModules)
                .map(ActiveModuleProvider::classToInstance)
                .collect(Collectors.toSet());
    }

    /**
     * Aggregate all resources from provided relative path into a {@code List<String>}
     */
    public static List<String> aggregateResources(final String relativePath, final ClassLoader classLoader) {
        try {
            return Collections.list(classLoader.getResources(relativePath)).stream()
                    .map(ActiveModuleProvider::toURI)
                    .flatMap(ActiveModuleProvider::folderToFile)
                    .map(File::toURI)
                    .map(Paths::get)
                    // readAll lines and add them to iteration
                    .flatMap(ActiveModuleProvider::readLines)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error("Unable to load resources from relative path {}", relativePath, e);
            throw new IllegalStateException("Unable to load resources from relative path " + relativePath, e);
        }
    }

    private static Stream<File> folderToFile(final URI uri) {
        final File[] files = new File(uri).listFiles(File::isFile);

        return files != null
                ? ImmutableList.copyOf(files).stream()
                : Collections.<File>emptyList().stream();
    }

    private static boolean filterNonModules(final Class<?> clazz) {
        final boolean isModule = Module.class.isAssignableFrom(clazz);
        if (!isModule) {
            LOG.warn("Class {} is provided in modules configuration, but is not a Module and will be ignored", clazz);
        }
        return isModule;
    }

    /**
     * Read lines from {@code Path}
     */
    private static Stream<String> readLines(final Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (IOException e) {
            LOG.error("Unable to read content of {}", path, e);
            throw new IllegalStateException("Unable to read content of " + path, e);
        }
    }

    /**
     * Converts {@code URL} to {@code URI}
     */
    private static URI toURI(final URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            LOG.error("Unable to convert {} to uri", url);
            throw new IllegalStateException("Unable to convert " + url + " to uri", e);
        }
    }

    /**
     * Loads class by provided name
     */
    private static Class<?> nameToClass(final String name,
                                        final ClassLoader classLoader) {
        try {
            LOG.info("Loading module class {}", name);
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            LOG.error("Unable to convert {} to class, make sure you've provided sources to classpath", name);
            throw new IllegalStateException(
                    "Unable to convert " + name + " to class, make sure you've provided sources to classpath", e);
        }
    }

    /**
     * Creates instance of module class
     */
    private static Module classToInstance(final Class<?> moduleClass) {
        try {
            LOG.info("Creating instance for module {}", moduleClass);
            return Module.class.cast(moduleClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            LOG.error("Unable to create instance for class {}", moduleClass, e);
            throw new IllegalStateException("Unable to create instance for class" + moduleClass, e);
        }
    }
}
