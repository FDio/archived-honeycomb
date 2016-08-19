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
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker

@Slf4j
class InitializerPipelineModule extends PrivateModule {


    public static final String HONEYCOMB_INITIALIZER = "honeycomb-initializer"

    protected void configure() {
        // Create data tree manager on top of non-persisting config data tree
        bind(ModifiableDataManager).toProvider(ModifiableDTDelegInitProvider).in(Singleton)
        // Wrap as DOMDataBroker
        bind(DOMDataBroker).toProvider(HoneycombDOMDataBrokerProvider).in(Singleton)
        // Wrap as BA data broker
        bind(DataBroker).annotatedWith(Names.named(HONEYCOMB_INITIALIZER)).toProvider(BindingDataBrokerProvider).in(Singleton)
        expose(DataBroker).annotatedWith(Names.named(HONEYCOMB_INITIALIZER))

        // Create initializer registry so that plugins can provide their initializers
        bind(InitializerRegistry)
                .annotatedWith(Names.named(HONEYCOMB_INITIALIZER))
                .toProvider(InitializerRegistryProvider)
                .in(Singleton)
        expose(InitializerRegistry)
                .annotatedWith(Names.named(HONEYCOMB_INITIALIZER))
    }
}
