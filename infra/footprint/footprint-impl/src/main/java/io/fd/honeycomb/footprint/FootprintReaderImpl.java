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

package io.fd.honeycomb.footprint;

import static java.lang.String.format;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FootprintReaderImpl implements FootprintReader {

    private static final Logger LOG = LoggerFactory.getLogger(FootprintReaderImpl.class);

    private final int pid;

    public FootprintReaderImpl() {
        pid = initPID();
        LOG.info("Footprint marker initialized for pid {}", pid);
    }


    private static int initPID() {
        final String processName = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(processName.substring(0, processName.indexOf("@")));
    }

    @Override
    public int readCurrentFootprint() {
        try {
            final Process process = Runtime.getRuntime().exec(" ps -eo rss,pid");

            final BufferedInputStream input = new BufferedInputStream(process.getInputStream());
            final String processOut = CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));

            final String pidLine = Arrays.stream(processOut.split(System.lineSeparator()))
                    .skip(1)// skip header
                    .map(String::trim)
                    .filter(line -> Integer.parseInt(line.split("\\s+")[1]) == pid)
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalStateException(format("Unable to find memory stats for pid %s", pid)));

            return Integer.parseInt(pidLine.split(" ")[0]);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getPid() {
        return pid;
    }
}
