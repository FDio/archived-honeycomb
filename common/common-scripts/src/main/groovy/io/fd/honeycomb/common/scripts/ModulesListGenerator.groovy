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

package io.fd.honeycomb.common.scripts

import groovy.text.SimpleTemplateEngine

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Generate modules-list file a honeycomb distribution.
 */
class ModulesListGenerator {

    static final def DEFAULT_MODULES_LIST = ModulesListGenerator.getResource("/modules/modulesListDefaultContent").text

    static final def MODULES_LIST_CONTENT_PROPERTY = "distribution.modules"
    static final def MODULES_FOLDER = "modules"
    static final def MODULE_LIST_FILE_SUFFIX = "-module-config"
    static final def SEPARATOR = ","

    public static void generate(project, properties, log) {
        // module configuration file extraction
        // builds project name from group,artifact and version to prevent overwriting
        // while building multiple distribution project
        def artifact = project.artifact
        def projectName = pathFriendlyProjectName(artifact)

        log.info "Generating list of modules started by distribution ${projectName}"

        def activeModules = properties.getProperty(MODULES_LIST_CONTENT_PROPERTY, DEFAULT_MODULES_LIST)
                .tokenize(SEPARATOR)
                .collect { module -> module.trim() }

        log.info "Project ${projectName} : Found modules ${activeModules}"
        //creates folder modules

        def outputPath = modulesConfigFolder(project)
        //creates module folder
        outputPath.toFile().mkdirs()

        def outputFile = Paths.get(outputPath.toString(), "${projectName}${MODULE_LIST_FILE_SUFFIX}").toFile()
        outputFile.createNewFile();
        log.info("Writing module configuration for distribution ${projectName} to ${outputPath}")

        if (activeModules.isEmpty()) {
            outputFile.text = new SimpleTemplateEngine().createTemplate(DEFAULT_MODULES_LIST).make(
                    ["groupId"   : project.groupId,
                     "artifactId": project.artifactId,
                     "version"   : project.version]).toString()
        } else {
            activeModules.add(0, "// Generated from ${project.groupId}/${project.artifactId}/${project.version}")
            outputFile.text = activeModules.join(System.lineSeparator)
        }
    }

    public static Path modulesConfigFolder(project) {
        return Paths.get(project.build.outputDirectory, StartupScriptGenerator.MINIMAL_RESOURCES_FOLDER, MODULES_FOLDER)
    }

    public static String pathFriendlyProjectName(artifact) {
        return "${artifact.getGroupId()}_${artifact.getArtifactId()}_${artifact.getVersion()}".replace(".", "-")
    }
}
