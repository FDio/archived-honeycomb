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

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.honeycomb.benchmark.util.DataProvider;
import io.fd.honeycomb.benchmark.util.FileManager;
import io.fd.honeycomb.benchmark.util.StaticReader;
import io.fd.honeycomb.infra.distro.activation.ActivationConfig;
import io.fd.honeycomb.infra.distro.activation.ActiveModules;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Measures the performance of CONFIG writes into BA DataBroker, backed by HC infrastructure and then NOOP writers.
 */
/*
 * Timeout for one run of benchmark method
 * */
@Timeout(time = 1)

/*
* 20 warmup iterations, each will run for 1 second. Serves to get more real-life results, as jvm has many internal
* optimizations that takes time to create profiles for.
* */
@Warmup(iterations = 20, time = 1)

/*
 100 measurement iteration, each will run for 1 second.
 It means that there will be 100 iterations, each will run tested method for 1 second and see how many iterations were
 possible
 */
@Measurement(iterations = 100, time = 1)

/*
* An instance will be allocated for each thread running the given test.
* */
@State(Scope.Thread)

/*
* Control code runs on one jvm, benchmark runs on different one to have isolation
* */
@Fork(1)

/*
* Measuring Maximum throughput of operations
* */
@BenchmarkMode(Mode.Throughput)
public class DataBrokerOperReadBenchmark extends AbstractModule implements FileManager {

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerOperReadBenchmark.class);

    /*
    * Type of data used in benchmark
    * */
    @Param({"OPERATIONAL"})
    private LogicalDatastoreType dsType;

    // Persistence does not make a difference when only reading operational
    @Param({"false"})
    private boolean persistence;

    /*
    * Data sample type used
    * */
    @Param({DataProvider.SIMPLE_CONTAINER, DataProvider.LIST_IN_CONTAINER, DataProvider.COMPLEX_LIST_IN_CONTAINER})
    private String data;
    private DataProvider dataProvider;

    // Infra modules to load
    private final Module[] modules = new Module[]{
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
            bind(ActivationConfig.class).toInstance(getActivationConfig());
            bind(ActiveModules.class).toInstance(
                    new ActiveModules(Arrays.stream(modules).map(Module::getClass).collect(Collectors.toSet())));
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

    private static ActivationConfig getActivationConfig() {
        final ActivationConfig activationConfig = new ActivationConfig();
        activationConfig.yangModulesIndexPath = "yang-mapping";
        return activationConfig;
    }
}
