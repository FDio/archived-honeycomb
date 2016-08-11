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

    protected void configure() {
        // Expose registries for plugin reader/writer factories
        bind(ModifiableWriterRegistryBuilder).toProvider(WriterRegistryProvider).in(Singleton)
        expose(ModifiableWriterRegistryBuilder)
        bind(ModifiableReaderRegistryBuilder).toProvider(ReaderRegistryProvider).in(Singleton)
        expose(ModifiableReaderRegistryBuilder)

        // Non persisting data tree
        bind(DataTree)
                .annotatedWith(Names.named("honeycomb-config-nopersist"))
                .toProvider(DataTreeProvider.ConfigDataTreeProvider)
                .in(Singleton)
        expose(DataTree).annotatedWith(Names.named("honeycomb-config-nopersist"))
        // Persisting data tree wrapper
        bind(DataTree)
                .annotatedWith(Names.named("honeycomb-config"))
                .toProvider(PersistingDataTreeProvider.ConfigPersistingDataTreeProvider)
                .in(Singleton)
        expose(DataTree).annotatedWith(Names.named("honeycomb-config"))

        bind(ModifiableDataManager).toProvider(ModifiableDTDelegProvider).in(Singleton)
        bind(ReadableDataManager).toProvider(ReadableDTDelegProvider).in(Singleton)
        expose(ReadableDataManager)

        def domBrokerProvider = new HoneycombDOMDataBrokerProvider()
//        bind(DOMDataBroker).annotatedWith(Names.named("honeycomb-config")).toProvider(domBrokerProvider).in(Singleton)
        // Bind also without annotation for easy private injection
        bind(DOMDataBroker).toProvider(domBrokerProvider).in(Singleton)

        bind(DataBroker).annotatedWith(Names.named("honeycomb-config")).toProvider(BindingDataBrokerProvider).in(Singleton)
        expose(DataBroker).annotatedWith(Names.named("honeycomb-config"))

        bind(DataTreeInitializer)
                .annotatedWith(Names.named("honeycomb-config"))
                .toProvider(PersistedFileInitializerProvider.PersistedConfigInitializerProvider)
                .in(Singleton)
        expose(DataTreeInitializer).annotatedWith(Names.named("honeycomb-config"))

        configureNotifications()
    }

    protected void configureNotifications() {
        bind(DOMNotificationRouter).toProvider(DOMNotificationServiceProvider).in(Singleton)
        expose(DOMNotificationRouter)
        bind(Broker).toProvider(HoneycombDOMBrokerProvider).in(Singleton)
        expose(Broker)
    }
}
