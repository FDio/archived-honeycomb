package io.fd.honeycomb.benchmark.script

import groovy.text.SimpleTemplateEngine

import java.nio.file.Files
import java.nio.file.Paths

class MemoryBenchmarkStartupScriptGenerator {

    static final def STARTUP_SCRIPT_ON_REST_TEMPLATE = MemoryBenchmarkStartupScriptGenerator.getResource("/memoryBenchmarkScript")
    static final def STARTUP_SCRIPT_NAME_BASE = "honeycomb-memory-footprint-benchmark-"

    static final def FOOTPRINT_TEST_CLASS = "io.fd.honeycomb.benchmark.memory.HoneycombFootprintTest"

    static final def OUTPUT_PATH_PARAM = "-DoutPath=\$(dirname \$0)/"
    static final def SAMPLE_SIZE_PARAM = "-DsampleSize="

    /**
     * Generate script to run io.fd.honeycomb.benchmark.memory.HoneycombWithDataTest with provided params
     * */
    public static void generateWithDataScript(project, log, String outputFileName, dataSampleSize) {
        log.info "Binding execution script for with-data benchmark[output=${outputFileName},sampleSize=${dataSampleSize}]"
        def scriptContent = new SimpleTemplateEngine().createTemplate(STARTUP_SCRIPT_ON_REST_TEMPLATE).make(
                [
                        "testParams"             : "${OUTPUT_PATH_PARAM}${outputFileName}-${dataSampleSize} ${SAMPLE_SIZE_PARAM}${dataSampleSize}",
                        "testClass"              : FOOTPRINT_TEST_CLASS
                ]).toString()
        flushScript(Paths.get(project.build.directory, "${STARTUP_SCRIPT_NAME_BASE}${dataSampleSize}"), scriptContent, log)
    }

    private static flushScript(filePath, content,log) {
        log.info "Saving script to path ${filePath}"
        def file = Files.createFile(filePath).toFile()

        file.text = content
        file.setExecutable(true)
    }
}
