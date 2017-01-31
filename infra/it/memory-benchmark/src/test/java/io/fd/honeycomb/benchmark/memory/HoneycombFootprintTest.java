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
package io.fd.honeycomb.benchmark.memory;

import io.fd.honeycomb.benchmark.memory.config.StaticHoneycombConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

/**
 * Test honeycomb memory footprint while containing amount of nodes specified by
 * parameter sampleSize.<br>
 * Test must be triggered by separated scripts, not by Parametrized runner,<br>
 * because of classes like WebSocketServer that is internally a singleton, therefore it holds static reference<br>
 * on itself and would fail on second run on precondition, because junit kills threads, but does not unload classes
 */
public class HoneycombFootprintTest implements BenchmarkFilesProvider {

    private static final String SAMPLE_SIZE_PROP = "sampleSize";
    private static final String OUTPUT_PROP = "outPath";

    private StaticHoneycombConfiguration staticHoneycombConfiguration;
    private String outputPath;

    @Before
    public void init() {
        outputPath = Objects.requireNonNull(System.getProperty(OUTPUT_PROP),
                "No output file path specified, make sure you've specified -D" + OUTPUT_PROP + " parameter");
    }

    @Test
    public void testHoneycombMemoryFootprintWithData() throws Exception {
        final int dataSampleSize = Integer.valueOf(Objects.requireNonNull(System.getProperty(SAMPLE_SIZE_PROP),
                "No sample data size defined, make sure you've specified -D" + SAMPLE_SIZE_PROP + " parameter"));

        staticHoneycombConfiguration = new StaticHoneycombConfiguration(
                generateNonEmptyConfigDataFile("temp-config", dataSampleSize),
                generateEmptyJsonFile("temp-empty-context"));

        new MemoryFootprintBenchmark(staticHoneycombConfiguration, outputPath).run();
    }

    @After
    public void destroyTest() throws IOException {
        // removes temp files,etc
        staticHoneycombConfiguration.close();
    }
}
