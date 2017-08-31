/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.common.scripts

import com.google.common.base.Strings
import com.google.common.io.Files
import io.fd.honeycomb.yang.YangModuleWhitelistReader
import org.apache.commons.io.FileUtils
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.jar.JarFile
import java.util.stream.Collectors

/**
 * Provides logic to generate:
 * <li><b>generateIndexForPresentModules()</b> - yang-modules-binding/yang-modules -
 * List of Yang modules used by project(classpath + deps)</li>
 * <li><b>pairDistributionModulesWithYangModules()</b> -  yang-mapping/FULL_PROJECT_NAME-yang-modules-index -
 * Index from Guice modules to Yang modules that are used by respective Guice module</li>
 * <br>
 * These files can be then included in jars and distribution resources to allow
 * conditional yang module loading according to list of Guice modules that are started by distribution
 * */
class ModuleYangIndexGenerator {

    private static final String YANG_MODEL_PROVIDER_NAME = YangModelBindingProvider.class.getName()
    private static
    final YANG_PROVIDERS_PATH = "META-INF/services/" + YANG_MODEL_PROVIDER_NAME
    private static final MODULES_DELIMITER = ","
    private static final CLASS_EXT = "class"
    private static final String[] EXTENSIONS = [CLASS_EXT]
    private static final YANG_MODULES_FOLDER = "yang-modules-binding"
    private static final YANG_MODULES_FILE_NAME = "yang-modules"
    private static final YANG_MAPPING_FOLDER = "yang-mapping"
    private static final YANG_MODULES_INDEX_FILE_NAME = "yang-modules-index"

    public static void generateIndexForPresentModules(project, log) {
        String skip = project.getProperties().get("skip.module.list.generation")
        if (Boolean.parseBoolean(skip)) {
            log.info "Skipping yang modules list generation for project ${project.getName()}"
            return
        }

        String whitelist = project.getProperties().get("yang.modules.whitelist")
        String yangModules;
        if (whitelist != null) {
            log.info "Using whitelist configuration for project ${project.getName()}"
            def whiteListPath = Paths.get(whitelist)
            if (!whiteListPath.toFile().exists()) {
                throw new IllegalStateException(format("Whitelist file on path %s does not exist", whitelist));
            }

            def reader = new YangModuleWhitelistReader()
            def yangModuleWhitelist = reader.read(whiteListPath);
            yangModules = yangModuleWhitelist.getModules().stream()
                    .map { module -> module.getBindingProviderName().trim()}
                    .collect()
                    .join(MODULES_DELIMITER)
        } else {
            log.info "Checking module providers for project ${project.getName()}"
            // checks module provides from dependencies
            // folder with extracted libs
            def libsFolder = Paths.get(project.getBuild().getDirectory(), "lib")
            if (!libsFolder.toFile().exists()) {
                // Plugin for collecting dependencies is executed from parent project,
                // therefore it will run also for parent, that does not have any depedencies(just dep management)
                // so lib folder wont be created
                log.info "Folder ${libsFolder} does not exist - No dependencies to process"
                return
            }

            yangModules = java.nio.file.Files.walk(libsFolder)
                    .map { path -> path.toFile() }
                    .filter { file -> file.isFile() }
                    .filter { file -> file.getName().endsWith(".jar") }
                    .map { file -> getModuleProviderContentFromApiJar(new JarFile(file), log) }
                    .filter { content -> !Strings.isNullOrEmpty(content.trim()) }
                    .collect().join(MODULES_DELIMITER)
        }

        log.info "Yang modules found : $yangModules"
        def outputDir = Paths.get(project.getBuild().getOutputDirectory(), YANG_MODULES_FOLDER).toFile()
        outputDir.mkdirs()
        def outputFile = Paths.get(outputDir.getPath(), YANG_MODULES_FILE_NAME).toFile()
        outputFile.createNewFile()
        Files.write(yangModules, outputFile, StandardCharsets.UTF_8)
        log.info "Yang modules configuration successfully written to ${outputFile.getPath()}"
    }

    /**
     * Loads module list of current distribution, and attempts
     * to pair them with yang module providers from either their classpath or direct/indirect dependencies.
     * */
    public static void pairDistributionModulesWithYangModules(project, log) {
        def modules = modulesList(project)
        if (modules.isEmpty()) {
            log.warn "No distribution modules defined, skipping"
            return
        }

        log.info "Pairing distribution modules ${modules} to yang modules"
        def moduleToYangModulesIndex = new HashMap<String, String>()

        log.info "Pairing against dependencies"
        // The rest of the modules is looked up in dependencies
        pairAgainsDependencyArtifacts(project, modules, log, moduleToYangModulesIndex)
        // for ex.: /target/honeycomb-minimal-resources/yang-mapping
        def yangMappingFolder = Paths.get(project.getBuild().getOutputDirectory(), StartupScriptGenerator.MINIMAL_RESOURCES_FOLDER, YANG_MAPPING_FOLDER).toFile()

        if (!yangMappingFolder.exists()) {
            yangMappingFolder.mkdir()
        }
        def outputFileName = "${ModulesListGenerator.pathFriendlyProjectName(project.artifact)}_$YANG_MODULES_INDEX_FILE_NAME"

        def outputFile = Paths.get(yangMappingFolder.getPath(), outputFileName).toFile()
        outputFile.createNewFile()

        def indexFileContent = moduleToYangModulesIndex.entrySet()
                .stream()
                .map { entry -> "GUICE_MODULE:${entry.getKey()}|YANG_MODULES:${entry.getValue()}${System.lineSeparator()}" }
                .collect(Collectors.joining())

        Files.write(indexFileContent, outputFile, StandardCharsets.UTF_8)
        if (!modules.isEmpty()) {
            log.warn "No yang configuration found for modules ${modules}"
        }
        log.info "Distribution to yang modules index successfully generated to $outputFile"

    }

    // provides list of modules for distribution, not from property, but already processed list from /modules folder.
    // this allows us to skip all validation that is present in modules list generation, and just take final list of modules
    private static Set<String> modulesList(project) {
        def modulesFolder = ModulesListGenerator.modulesConfigFolder(project).toFile()
        Arrays.stream(modulesFolder.listFiles())
        // picks up only file for currently processed distribution
                .filter { file -> file.getName().contains(ModulesListGenerator.pathFriendlyProjectName(project.artifact)) }
                .map { file -> FileUtils.readLines(file, StandardCharsets.UTF_8) }
                .flatMap { lines -> lines.stream() }
                .map { line -> line.replace("//", "") }
                .map { line -> line.trim() }
                .collect(Collectors.toSet())
    }

    private static void pairAgainsDependencyArtifacts(project, modules, log, index) {
        // loads jar file
        def artifacts = project.getDependencyArtifacts()
        log.info "Artifacts used for pairing $artifacts"
        artifacts.stream()
                .map { artifact -> artifact.getFile() }
                .map { file -> new JarFile(file) }
                .forEach { jar ->
            // first tries to find content of yang module provides file,
            // if not found, skip's this jar
            def moduleProvidersContent = getModuleProviderContentFromImplJar(jar, log)
            if (Strings.isNullOrEmpty(moduleProvidersContent.trim())) {
                log.debug "No yang module configuration found in ${jar.getName()}"
                return
            }

            def entryNames = Collections.list(jar.entries()).stream()
                    .map { entry -> entry.getName() }
                    .filter { name -> name.endsWith(CLASS_EXT) }
                    .map { name -> pathToClassName(name) }
                    .collect(Collectors.toSet())

            log.info "Entries $entryNames"
            log.info "Modules $modules"
            for (String module : modules) {
                if (entryNames.contains(module)) {
                    log.info "Module $module found in artifact ${jar.getName()}"
                    index.put(module, moduleProvidersContent)
                }
            }
        }
        modules.removeAll(index.keySet());
        log.info "Modules left after dependency pairing $modules"
    }

    private static String relativizePath(String path, String outputDir) {
        return path.replace(outputDir, "").substring(1).trim();
    }

    private static String pathToClassName(String path) {
        return path.replace("/", ".").replace(".class", "").trim()
    }

    private static String classNameToPath(String className) {
        return className.replace(".", "/").concat(".class").trim()
    }

    private static String getModuleProviderContentFromImplJar(JarFile jarFile, log) {
        def moduleProviderEntry = jarFile.getJarEntry(YANG_MODULES_FOLDER + "/" + YANG_MODULES_FILE_NAME)
        if (moduleProviderEntry == null) {
            return "";
        }
        // module provider files are in general a couple of lines, so should'nt be a problem
        // to read at once
        InputStream input = jarFile.getInputStream(moduleProviderEntry)
        byte[] data = new byte[(int) moduleProviderEntry.getSize()]
        input.read(data)
        input.close()

        return fixDelimiters(new String(data, StandardCharsets.UTF_8));
    }

    private static String getModuleProviderContentFromApiJar(JarFile jarFile, log) {
        def moduleProviderEntry = jarFile.getJarEntry(YANG_PROVIDERS_PATH)
        if (moduleProviderEntry == null) {
            return "";
        }
        // module provider files are in general a couple of lines, so should'nt be a problem
        // to read at once
        InputStream input = jarFile.getInputStream(moduleProviderEntry)
        byte[] data = new byte[(int) moduleProviderEntry.getSize()]
        input.read(data)
        input.close()

        return fixDelimiters(new String(data, StandardCharsets.UTF_8))
    }

    private static String fixDelimiters(String data) {
        return Arrays.stream(data.split(System.lineSeparator()))
                .map { line -> line.trim() }
                .collect().join(MODULES_DELIMITER)
    }
}
