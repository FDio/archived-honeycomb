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
import io.fd.honeycomb.benchmark.util.DataSubmitter;
import io.fd.honeycomb.benchmark.util.FileManager;
import io.fd.honeycomb.benchmark.util.NoopWriter;
import io.fd.honeycomb.infra.distro.activation.ActivationConfig;
import io.fd.honeycomb.infra.distro.activation.ActiveModules;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedList;
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
public class DataBrokerConfigWriteBenchmark extends AbstractModule implements FileManager {

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerConfigWriteBenchmark.class);

    /*
    * This params says after how many operations should commit to data tree be executed
    * */
    @Param({"1", "10"/*, "100"*/})
    private int submitFrequency;

    /*
    * Enables/disables persisting content of transaction
    * */
    @Param({"true", "false"})
    private boolean persistence;

    /*
    * Crud operation used in benchmark
    * */
    @Param({"put"/*, "merge"*/})
    private String operation;
    private DataSubmitter submitter;

    /*
    * Type of data used
    * */
    @Param({"CONFIGURATION"})
    private LogicalDatastoreType dsType;

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
            new io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule(),
            new io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule(),
            this};

    private List<NoopWriter<?>> noopWriters = new ArrayList<>();
    private DataBroker dataBroker;
    private long counter = 0;
    private WriteTransaction tx;
    private HoneycombConfiguration instance;

    @Setup(Level.Iteration)
    public void setup() {
        LOG.info("Setting up");
        submitter = DataSubmitter.from(operation);
        dataProvider = DataProvider.from(data);
        Injector injector = Guice.createInjector(modules);
        final HoneycombConfiguration cfg = injector.getInstance(HoneycombConfiguration.class);
        LOG.info("Configuration for Honeycomb: {}", cfg);
        dataBroker = injector.getInstance(Key.get(DataBroker.class,
                Names.named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG)));
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        LOG.info("Tearing down after {} executions", counter);
        counter = 0;
        LOG.info("Writer invocations: {}", noopWriters);
        noopWriters.clear();

        tx = null;
        dataBroker = null;
        deleteFile(Paths.get(instance.peristConfigPath));
        deleteFile(Paths.get(instance.peristContextPath));
    }

    @Benchmark
    public void write() {
        // Count executions
        counter++;

        // New transaction after it was committed
        if (tx == null) {
            tx = dataBroker.newWriteOnlyTransaction();
        }

        submitter.submit(dsType, tx, dataProvider.getId(counter), dataProvider.getData(counter));

        // Commit based on frequency set
        if (counter % submitFrequency == 0) {
            try {
                tx.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Submit failed", e);
            }
            tx = null;
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

        final Multibinder<WriterFactory> writeBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writeBinder.addBinding().toInstance(registry -> {
            // Add noop writers for all data written in this benchmark
            addWriter(registry, new NoopWriter<>(InstanceIdentifier.create(SimpleContainer.class)));
            addWriter(registry, new NoopWriter<>(InstanceIdentifier.create(ContainerWithList.class)));
            addWriter(registry, new NoopWriter<>(InstanceIdentifier.create(ContainerWithList.class)
                    .child(ListInContainer.class)));
            addWriter(registry, new NoopWriter<>(InstanceIdentifier.create(ContainerWithList.class)
                    .child(ListInContainer.class)
                    .child(ContainerInList.class)));
            addWriter(registry, new NoopWriter<>(InstanceIdentifier.create(ContainerWithList.class)
                    .child(ListInContainer.class)
                    .child(ContainerInList.class)
                    .child(NestedList.class)));
        });
    }

    private void addWriter(final ModifiableWriterRegistryBuilder registry, final NoopWriter<?> handler) {
        noopWriters.add(handler);
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
