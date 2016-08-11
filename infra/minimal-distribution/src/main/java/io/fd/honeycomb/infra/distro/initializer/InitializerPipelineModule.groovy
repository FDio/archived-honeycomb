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

package io.fd.honeycomb.infra.distro.initializer

import com.google.inject.PrivateModule
import com.google.inject.Singleton
import com.google.inject.name.Names
import groovy.util.logging.Slf4j
import io.fd.honeycomb.data.ModifiableDataManager
import io.fd.honeycomb.data.init.InitializerRegistry
import io.fd.honeycomb.infra.distro.data.BindingDataBrokerProvider
import io.fd.honeycomb.infra.distro.data.HoneycombDOMDataBrokerProvider
import io.fd.honeycomb.infra.distro.data.ModifiableDTDelegProvider
import io.fd.honeycomb.translate.util.write.NoopWriterRegistry
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker

@Slf4j
class InitializerPipelineModule extends PrivateModule {

    protected void configure() {
        // Initializer hierarchy uses fake writer registry to not send restored configuration from plugins back to plugins
        def registry = new NoopWriterRegistry()
        bind(ModifiableWriterRegistryBuilder)
                .annotatedWith(Names.named("honeycomb-initializer"))
                .toInstance([build: { registry }])

        // Then just build data tree delegator and DataBrokers on top of it
        bind(ModifiableDataManager).toProvider(ModifiableDTDelegProvider).in(Singleton)
        bind(DOMDataBroker).toProvider(HoneycombDOMDataBrokerProvider).in(Singleton)
        bind(DataBroker).annotatedWith(Names.named("honeycomb-initializer")).toProvider(BindingDataBrokerProvider).in(Singleton)
        expose(DataBroker).annotatedWith(Names.named("honeycomb-initializer"))

        // NOW do initializer registry
        bind(InitializerRegistry)
                .annotatedWith(Names.named("honeycomb-initializer"))
                .toProvider(InitializerRegistryProvider)
                .in(Singleton)
        expose(InitializerRegistry)
                .annotatedWith(Names.named("honeycomb-initializer"))
    }
}
