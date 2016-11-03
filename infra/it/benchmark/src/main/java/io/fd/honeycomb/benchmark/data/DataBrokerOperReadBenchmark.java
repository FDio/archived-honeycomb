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

package io.fd.honeycomb.benchmark.data;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.honeycomb.benchmark.util.DataProvider;
import io.fd.honeycomb.benchmark.util.FileManager;
import io.fd.honeycomb.benchmark.util.StaticReader;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Measures the performance of CONFIG writes into BA DataBroker, backed by HC infrastructure and then NOOP writers.
 */
@Timeout(time = 20)
@Warmup(iterations = 2, time = 10)
@Measurement(iterations = 2, time = 10)
@Fork(2)
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
public class DataBrokerOperReadBenchmark extends AbstractModule implements FileManager {

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerOperReadBenchmark.class);

    @Param({"OPERATIONAL"})
    private LogicalDatastoreType dsType;

    // Persistence does not make a difference when only reading operational
    @Param({"false"})
    private boolean persistence;

    @Param({DataProvider.SIMPLE_CONTAINER, DataProvider.LIST_IN_CONTAINER, DataProvider.COMPLEX_LIST_IN_CONTAINER})
    private String data;
    private DataProvider dataProvider;

    /*
    * TODO HONEYCOMB-288 Visualization notes:
    * - visualize as 1 graph
    * - just 3 lines
    */

    // Infra modules to load
    private final Module[] modules = new Module[] {
            new io.fd.honeycomb.infra.distro.schema.YangBindingProviderModule(),
            new io.fd.honeycomb.infra.distro.schema.SchemaModule(),
            new ConfigAndOperationalPipelineModule(),
            new io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule(),
            this};

    private List<Reader<?, ?>> noopReaders = new ArrayList<>();
    private DataBroker dataBroker;
    private long counter = 0;
    private ReadOnlyTransaction tx;
    private HoneycombConfiguration instance;

    @Setup(Level.Iteration)
    public void setup() {
        LOG.info("Setting up");
        dataProvider = DataProvider.from(data);
        Injector injector = Guice.createInjector(modules);
        final HoneycombConfiguration cfg = injector.getInstance(HoneycombConfiguration.class);
        LOG.info("Configuration for Honeycomb: {}", cfg);
        dataBroker = injector.getInstance(Key.get(DataBroker.class,
                Names.named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG)));
        tx = dataBroker.newReadOnlyTransaction();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        LOG.info("Tearing down after {} executions", counter);
        counter = 0;
        LOG.info("Reader invocations: {}", noopReaders);
        noopReaders.clear();

        tx.close();
        tx = null;
        dataBroker = null;
        deleteFile(Paths.get(instance.peristConfigPath));
        deleteFile(Paths.get(instance.peristContextPath));
    }

    @Benchmark
    public void read() {
        try {
            tx.read(dsType, dataProvider.getId(counter++)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Read failed", e);
        }
    }

    /**
     * Inject custom modules e.g. configuration.
     */
    @Override
    protected void configure() {
        try {
            instance = getHoneycombConfiguration(persistence);
            bind(HoneycombConfiguration.class).toInstance(instance);
        } catch (IOException e) {
            throw new RuntimeException("Unable to prepare configuration", e);
        }

        final Multibinder<ReaderFactory> writeBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        writeBinder.addBinding().toInstance(registry -> {

            switch (data) {
                case DataProvider.SIMPLE_CONTAINER: {
                    addReader(registry, new StaticReader<>(
                            InstanceIdentifier.create(SimpleContainer.class),
                            DataProvider.from(DataProvider.SIMPLE_CONTAINER)));
                    break;
                }
                case DataProvider.LIST_IN_CONTAINER: {
                    registry.addStructuralReader(
                            InstanceIdentifier.create(ContainerWithList.class), ContainerWithListBuilder.class);
                    addReader(registry, new StaticReader<>(
                            InstanceIdentifier.create(ContainerWithList.class).child(ListInContainer.class),
                            DataProvider.from(DataProvider.LIST_IN_CONTAINER)));
                    break;
                }
                case DataProvider.COMPLEX_LIST_IN_CONTAINER: {
                    registry.addStructuralReader(
                            InstanceIdentifier.create(ContainerWithList.class), ContainerWithListBuilder.class);
                    addReader(registry, new StaticReader<>(
                            InstanceIdentifier.create(ContainerWithList.class).child(ListInContainer.class),
                            DataProvider.from(DataProvider.COMPLEX_LIST_IN_CONTAINER)));
                    break;
                }
            }
        });
    }

    private void addReader(final ModifiableReaderRegistryBuilder registry, final Reader<?, ?> handler) {
        noopReaders.add(handler);
        registry.add(handler);
    }

    private static HoneycombConfiguration getHoneycombConfiguration(final boolean persistence) throws IOException {
        final HoneycombConfiguration instance = new HoneycombConfiguration();
        instance.persistConfig = Optional.of(Boolean.toString(persistence));
        instance.persistContext = Optional.of(Boolean.toString(persistence));
        instance.peristConfigPath = FileManager.INSTANCE.createTempFile("config").toString();
        instance.peristContextPath = FileManager.INSTANCE.createTempFile("context").toString();
        return instance;
    }
}
