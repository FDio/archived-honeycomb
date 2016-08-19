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

package io.fd.honeycomb.infra.distro.data

import com.google.inject.PrivateModule
import com.google.inject.Singleton
import com.google.inject.name.Names
import groovy.util.logging.Slf4j
import io.fd.honeycomb.data.ModifiableDataManager
import io.fd.honeycomb.data.ReadableDataManager
import io.fd.honeycomb.data.init.DataTreeInitializer
import io.fd.honeycomb.infra.distro.data.config.WriterRegistryProvider
import io.fd.honeycomb.infra.distro.data.oper.ReadableDTDelegProvider
import io.fd.honeycomb.infra.distro.data.oper.ReaderRegistryProvider
import io.fd.honeycomb.infra.distro.initializer.PersistedFileInitializerProvider
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter
import org.opendaylight.controller.sal.core.api.Broker
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree

@Slf4j
class ConfigAndOperationalPipelineModule extends PrivateModule {

    public static final String HONEYCOMB_CONFIG_NONPERSIST = "honeycomb-config-nopersist"
    public static final String HONEYCOMB_CONFIG = "honeycomb-config"

    protected void configure() {
        // Expose registries for plugin reader/writer factories
        bind(ModifiableWriterRegistryBuilder).toProvider(WriterRegistryProvider).in(Singleton)
        expose(ModifiableWriterRegistryBuilder)
        bind(ModifiableReaderRegistryBuilder).toProvider(ReaderRegistryProvider).in(Singleton)
        expose(ModifiableReaderRegistryBuilder)

        // Non persisting data tree for config
        bind(DataTree)
                .annotatedWith(Names.named(HONEYCOMB_CONFIG_NONPERSIST))
                .toProvider(DataTreeProvider.ConfigDataTreeProvider)
                .in(Singleton)
        expose(DataTree).annotatedWith(Names.named(HONEYCOMB_CONFIG_NONPERSIST))
        // Persisting data tree wrapper for config
        bind(DataTree)
                .annotatedWith(Names.named(HONEYCOMB_CONFIG))
                .toProvider(PersistingDataTreeProvider.ConfigPersistingDataTreeProvider)
                .in(Singleton)
        expose(DataTree).annotatedWith(Names.named(HONEYCOMB_CONFIG))

        // Config Data Tree manager working on top of config data tree + writer registry
        bind(ModifiableDataManager).toProvider(ModifiableDTDelegProvider).in(Singleton)
        // Operational Data Tree manager working on top of reader registry
        bind(ReadableDataManager).toProvider(ReadableDTDelegProvider).in(Singleton)
        expose(ReadableDataManager)

        // DOMDataBroker wrapper on top of data tree managers
        def domBrokerProvider = new HoneycombDOMDataBrokerProvider()
        bind(DOMDataBroker).toProvider(domBrokerProvider).in(Singleton)

        // BA version of data broker
        bind(DataBroker).annotatedWith(Names.named(HONEYCOMB_CONFIG)).toProvider(BindingDataBrokerProvider).in(Singleton)
        expose(DataBroker).annotatedWith(Names.named(HONEYCOMB_CONFIG))

        // Create initializer to init persisted config data
        bind(DataTreeInitializer)
                .annotatedWith(Names.named(HONEYCOMB_CONFIG))
                .toProvider(PersistedFileInitializerProvider.PersistedConfigInitializerProvider)
                .in(Singleton)
        expose(DataTreeInitializer).annotatedWith(Names.named(HONEYCOMB_CONFIG))

        configureNotifications()
    }

    protected void configureNotifications() {
        // Create notification service
        bind(DOMNotificationRouter).toProvider(DOMNotificationServiceProvider).in(Singleton)
        expose(DOMNotificationRouter)
        // Wrap notification service, data broker and schema service in a Broker MD-SAL API
        bind(Broker).toProvider(HoneycombDOMBrokerProvider).in(Singleton)
        expose(Broker)
    }
}
