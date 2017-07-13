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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import io.fd.honeycomb.benchmark.memory.config.BindableCfgAttrsModule;
import io.fd.honeycomb.benchmark.memory.config.StaticActivationModule;
import io.fd.honeycomb.infra.distro.Main;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import io.fd.honeycomb.management.jmx.JMXBeanProvider;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import java.util.Set;

/**
 * Measure memory consumption of config data storage
 */
public class MemoryFootprintBenchmark implements JMXBeanProvider, BenchmarkFilesProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryFootprintBenchmark.class);

    // configuration class used to run benchmark, allows us to switch between honeycomb with data, or on rest
    private final HoneycombConfiguration configuration;

    // output file path
    private final String outputPath;

    public MemoryFootprintBenchmark(@Nonnull final HoneycombConfiguration configuration, @Nonnull final String outputPath) {
        this.configuration = configuration;
        this.outputPath = outputPath;
    }

    public void run() throws Exception {
        // start honeycomb with configuration of BASE_MODULES + configuration class
        final Injector injector = startHoneycomb();

        // query memory beans with JMX and output results on output path
        queryMemoryBeans(injector.getInstance(JMXServiceURL.class))
                .forEach(memoryInfo -> outputBenchmarkResult(memoryInfo, outputPath, LOG));
        // shutdowns server instance
        injector.getInstance(Server.class).stop();
    }

    /**
     * start honeycomb with basic modules + provided static configuration
     */
    private Injector startHoneycomb() {
        LOG.info("Starting embedded server with configuration {}", configuration);
        return Main.init(new StaticActivationModule(new BindableCfgAttrsModule(configuration)));
    }

    /**
     * Queries heap and non-heap memory usage
     */
    private Set<MemoryInfo> queryMemoryBeans(final JMXServiceURL url) {
        LOG.info("Requesting memory bean on url {}", url);

        try (final JMXConnector connector = getConnector(url)) {
            MemoryInfo heapMemoryInfo = new MemoryInfo(
                    (CompositeDataSupport) getJMXAttribute(connector, MemoryInfo.MEMORY_MBEAN_TYPE,
                            MemoryInfo.HEAP_MEMORY), MemoryInfo.HEAP_MEMORY);
            LOG.info("Heap memory usage {}", heapMemoryInfo);

            MemoryInfo nonHeapMemoryInfo = new MemoryInfo(
                    (CompositeDataSupport) getJMXAttribute(connector, MemoryInfo.MEMORY_MBEAN_TYPE,
                            MemoryInfo.NON_HEAP_MEMORY), MemoryInfo.NON_HEAP_MEMORY);
            LOG.info("NonHeap memory usage {}", nonHeapMemoryInfo);
            return ImmutableSet.of(heapMemoryInfo, nonHeapMemoryInfo);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to query memory beans", e);
        }
    }

}

