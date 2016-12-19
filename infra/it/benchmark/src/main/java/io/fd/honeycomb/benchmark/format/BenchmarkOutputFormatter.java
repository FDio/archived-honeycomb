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

package io.fd.honeycomb.benchmark.format;

import com.google.common.base.Charsets;
import io.fd.honeycomb.benchmark.util.DataProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes JMH CSV file to format accepted by Jenkins plot plugin (header + one data line per CSV file).
 */
public final class BenchmarkOutputFormatter {
    /*
    Input format of the JMH CSV file:

    "Benchmark","Mode","Threads","Samples","Score","Score Error (99,9%)","Unit","Param: data","Param: dsType","Param: operation","Param: persistence","Param: submitFrequency"
    "io.fd.honeycomb.benchmark.data.DataBrokerConfigWriteBenchmark.write","thrpt",1,1,"3099,563546",NaN,"ops/s",simple-container,CONFIGURATION,put,true,1
    ...

     */
    private static final int SCORE_POSITION = 4;
    private static final int DATA_TYPE_POSITION = 7;
    private static final int DS_TYPE_POSITION = 8;
    private static final int PERSISTENCE_POSITION = 10;
    private static final int SUBMIT_FREQUENCY_POSITION = 11;

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkOutputFormatter.class);

    private BenchmarkOutputFormatter() {
    }

    /**
     * Produces 4 CSV files (simple-container, list-in-container, complex-list-in-container write + one plot for read).
     */
    public static void main(final String[] args) throws Exception {
        final File csvFile = new File(args[0]);
        if (!csvFile.exists()) {
            throw new FileNotFoundException(args[0]);
        }
        final String path = csvFile.getParent();
        LOG.info("Preparing benchmarking plot data from: {}", args[0]);

        final Reader in = new InputStreamReader(new FileInputStream(csvFile), Charsets.UTF_8);
        final List<CSVRecord> records = CSVFormat.RFC4180.parse(in).getRecords();
        writeStatistics(processSimpleContainer(records), path, "simple-container.csv");
        writeStatistics(processListContainer(records), path, "list-in-container.csv");
        writeStatistics(processComplexListContainer(records), path, "complex-list-in-container.csv");
        writeStatistics(processReadStatistics(records), path, "operational-read.csv");
        LOG.info("Finished benchmarking plot data preparation");
    }

    private static boolean isConfiguration(CSVRecord record) {
        return LogicalDatastoreType.CONFIGURATION.toString().equals(record.get(DS_TYPE_POSITION));
    }

    private static List<DataEntry> processSimpleContainer(final List<CSVRecord> list) {
        return list.stream().filter(
            record -> DataProvider.SIMPLE_CONTAINER.equals(record.get(DATA_TYPE_POSITION)) && isConfiguration(record))
            .map(DataEntry::parseWriteData).collect(Collectors.toList());
    }

    private static List<DataEntry> processListContainer(final List<CSVRecord> list) {
        return list.stream().filter(
            record -> DataProvider.LIST_IN_CONTAINER.equals(record.get(DATA_TYPE_POSITION)) && isConfiguration(record))
            .map(DataEntry::parseWriteData)
            .collect(Collectors.toList());
    }

    private static List<DataEntry> processComplexListContainer(final List<CSVRecord> list) {
        return list.stream().filter(
            record -> DataProvider.COMPLEX_LIST_IN_CONTAINER.equals(record.get(DATA_TYPE_POSITION))
                && isConfiguration(record))
            .map(DataEntry::parseWriteData)
            .collect(Collectors.toList());
    }

    private static List<DataEntry> processReadStatistics(final List<CSVRecord> list) {
        return list.stream()
            .filter(record -> LogicalDatastoreType.OPERATIONAL.toString().equals(record.get(DS_TYPE_POSITION)))
            .map(DataEntry::parseReadData)
            .collect(Collectors.toList());
    }

    private static void writeStatistics(final List<DataEntry> data, final String path, final String fileName)
        throws IOException {
        final String absolutePath = path + '/' + fileName;
        LOG.debug("Writing benchmark statistics to file {}", absolutePath);
        final List<String> keys = new ArrayList<>();
        final List<String> scores = new ArrayList<>();
        data.stream().forEach(entry -> {
            keys.add(entry.key);
            scores.add(entry.score);
        });
        LOG.debug("header: {}", keys);
        LOG.debug("values: {}", scores);

        final StringBuilder buffer = new StringBuilder();
        final CSVPrinter csv = new CSVPrinter(buffer, CSVFormat.RFC4180);
        csv.printRecord(keys);
        csv.printRecord(scores);
        csv.close();

        try (final FileOutputStream out = new FileOutputStream(absolutePath)) {
            out.write(buffer.toString().getBytes(Charsets.UTF_8));
            out.close();
            LOG.debug("Statistics written successfully");
        }
    }

    private static final class DataEntry {
        private final String key;
        private final String score;

        private DataEntry(final String key, final String score) {
            this.key = key;
            this.score = score;
        }

        static DataEntry parseWriteData(final CSVRecord record) {
            return new DataEntry(
                "persistence=" + record.get(PERSISTENCE_POSITION) + " freq=" + record.get(SUBMIT_FREQUENCY_POSITION),
                record.get(SCORE_POSITION)
            );
        }

        static DataEntry parseReadData(final CSVRecord record) {
            return new DataEntry(
                record.get(DATA_TYPE_POSITION),
                record.get(SCORE_POSITION)
            );
        }
    }
}
