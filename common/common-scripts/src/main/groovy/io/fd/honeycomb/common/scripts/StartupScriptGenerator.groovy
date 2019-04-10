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
 * Generate startup shell scripts for a honeycomb distribution.
 */
class StartupScriptGenerator {

    static final def DEFAULT_START_SCRIPT_TEMPLATE = StartupScriptGenerator.getResource("/scripts/startScript").text
    static final def FORK_SCRIPT_TEMPLATE = StartupScriptGenerator.getResource("/scripts/forkScript").text
    static final def KILL_SCRIPT_TEMPLATE = StartupScriptGenerator.getResource("/scripts/killScript").text
    static final def README_TEMPLATE = StartupScriptGenerator.getResource("/scripts/README").text

    static final def JVM_PARAMS_KEY = "exec.parameters"
    static final def DEFAULT_ADDITIONAL_JVM_PROPERTIES = ""
    static final def JVM_DEBUG_PARAMS_KEY = "debug.parameters"
    static final def START_SCRIPT_TEMPLATE_KEY = "start.script.template"
    static final def MINIMAL_RESOURCES_FOLDER = "honeycomb-minimal-resources"

    static final def STARTUP_SCRIPT_NAME = "honeycomb"
    static final def KILL_SCRIPT_NAME = "honeycomb-kill"
    static final def FORK_STARTUP_SCRIPT_NAME = "honeycomb-start"
    static final def DEFAULT_DEBUG_JVM_PARAMS = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

    public static void generate(project, properties, log) {
        log.info "Generating honeycomb shell scripts for ${project.artifactId}"

        // JVM params, defined in pom or no special params
        def additionalJvmParameters = properties.getOrDefault(JVM_PARAMS_KEY, DEFAULT_ADDITIONAL_JVM_PROPERTIES)
        log.debug "Additional JVM properties: ${additionalJvmParameters}"
        // Startup script template, can be overridden by property in pom
        def scriptTemplate = properties.getOrDefault(START_SCRIPT_TEMPLATE_KEY, DEFAULT_START_SCRIPT_TEMPLATE)
        log.debug "Template used for startup script: ${scriptTemplate}"
        // JVM debug params, defined in pom or no special params
        def debugJvmParameters = properties.getOrDefault(JVM_DEBUG_PARAMS_KEY, DEFAULT_DEBUG_JVM_PARAMS)
        log.debug "Debug JVM properties: ${additionalJvmParameters}"

        def jarName = "${project.artifactId}-${project.version}.jar"
        def jvmParameters = "${additionalJvmParameters} -jar \$(dirname \$0)/${jarName}"
        def scriptParent = Paths.get(project.build.outputDirectory as String, MINIMAL_RESOURCES_FOLDER)
        scriptParent.toFile().mkdirs()

        def startScriptPath = generateStartupScript(jvmParameters, log, scriptParent, scriptTemplate)
        def forkScriptPath = generateForkingStartupScript(scriptParent, log)
        def debugScriptPath = generateDebugStartupScript(debugJvmParameters, jvmParameters, log, scriptParent, scriptTemplate)
        def killScriptPath = generateKillScript(jarName, log, scriptParent)
        generateReadme(scriptParent, log, startScriptPath, forkScriptPath, debugScriptPath, killScriptPath, project)
    }

    private static def generateReadme(scriptParent, log,
                                      startScriptPath, forkScriptPath, debugScriptPath, killScriptPath, project) {
        def readmePath = Paths.get(scriptParent.toString(), "README")

        def readmeContent = new SimpleTemplateEngine().createTemplate(README_TEMPLATE).make(
                ["groupId"    : project.groupId,
                 "artifactId" : project.artifactId,
                 "version"    : project.version,
                 "startScript": startScriptPath.fileName,
                 "forkScript" : forkScriptPath.fileName,
                 "debugScript": debugScriptPath.fileName,
                 "killScript" : killScriptPath.fileName]).toString()
        log.info "Writing README to ${readmePath}"
        flushScript(readmePath, readmeContent)
    }

    private static def generateDebugStartupScript(debugJvmParameters, javaArgs, log, Path scriptParent,
                                                  scriptTemplate) {
        def exec = "java ${debugJvmParameters} ${javaArgs}"
        log.info "Debug script content to be used: ${exec}"
        def scriptPath = Paths.get(scriptParent.toString(), "honeycomb-debug")
        log.info "Writing shell debug script to ${scriptPath}"
        flushScript(scriptPath, new SimpleTemplateEngine().createTemplate(scriptTemplate).make(["exec": exec]).toString())
    }

    private static def generateForkingStartupScript(scriptParent, log) {
        def scriptPath = Paths.get(scriptParent.toString(), FORK_STARTUP_SCRIPT_NAME)
        log.info "Writing forking startup script to ${scriptPath}"
        flushScript(scriptPath, new SimpleTemplateEngine().createTemplate(FORK_SCRIPT_TEMPLATE).make().toString())
    }

    private static def flushScript(filePath, content) {
        filePath.toFile().text = content
        filePath.toFile().setExecutable(true)
        filePath
    }

    private static def generateStartupScript(javaArgs, log, scriptParent, scriptTemplate) {
        def exec = "java ${javaArgs}"
        log.info "Startup script content to be used: ${exec}"
        def scriptPath = Paths.get(scriptParent.toString(), STARTUP_SCRIPT_NAME)
        log.info "Writing startup script to ${scriptPath}"
        flushScript(scriptPath, new SimpleTemplateEngine().createTemplate(scriptTemplate).make(["exec": exec]).toString())
    }

    private static def generateKillScript(jarName, log, scriptParent) {
        def pattern = "java.*${jarName}"
        def scriptPath = Paths.get(scriptParent.toString(), KILL_SCRIPT_NAME)
        log.info "Writing kill script to ${scriptPath}"
        flushScript(scriptPath, new SimpleTemplateEngine().createTemplate(KILL_SCRIPT_TEMPLATE).make(["pattern": pattern]).toString())
    }
}
