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


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;

public interface BenchmarkFilesProvider {

    default void outputBenchmarkResult(@Nonnull final MemoryInfo benchmarkResult,
                                       @Nonnull final String outputPath,
                                       @Nonnull final Logger logger) {
        // specifies output file in form specified_name-memory_info_type.csv
        final Path outPath = Paths.get(outputPath + "-" + benchmarkResult.getMemoryInfoTypeName() + ".csv");
        final CSVFormat csvFormat = CSVFormat.RFC4180.withHeader(MemoryInfo.COMMITTED, MemoryInfo.INIT, MemoryInfo.MAX, MemoryInfo.USED);

        try (final CSVPrinter csvPrinter = new CSVPrinter(new StringBuilder(), csvFormat)) {
            // prints values in same order that header is
            csvPrinter.printRecord(benchmarkResult.getCommitted(), benchmarkResult.getInit(), benchmarkResult.getMax(), benchmarkResult.getUsed());

            logger.info("Creating output file {}", outPath);
            // writes output to separate file
            Files.write(Files.createFile(outPath), Collections.singleton(csvPrinter.getOut().toString()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to output results of benchmark", e);
        }
    }


    default String generateEmptyJsonFile(final String fileName) {
        try {
            Path tempFilePath = Files.createTempFile(fileName, ".json");

            Files.write(tempFilePath, Arrays.asList("{}"));

            return tempFilePath.normalize().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create temp config data file");
        }
    }

    /**
     * Generate dummy data file to be provided for honeycomb as config data
     */
    default String generateNonEmptyConfigDataFile(final String fileName, final int dataSampleSize) {
        try {
            Path tempFilePath = Files.createTempFile(fileName, ".json");

            StringBuilder dataBuilder = new StringBuilder();

            dataBuilder.append("{\"mm-bench:config-data\":{\"config-list\":[");

            for (int i = 0; i < dataSampleSize; i++) {
                dataBuilder.append("{\"name\":\"")
                        .append(String.valueOf(i))
                        .append("\"}");
                if (i != dataSampleSize - 1) {
                    dataBuilder.append(",");
                }
            }

            dataBuilder.append("]}}");

            Files.write(tempFilePath, Arrays.asList(dataBuilder.toString()));

            return tempFilePath.normalize().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create temp config data file");
        }
    }
}
