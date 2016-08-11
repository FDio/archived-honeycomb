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

package io.fd.honeycomb.infra.distro.data.context

import com.google.inject.PrivateModule
import com.google.inject.Singleton
import com.google.inject.name.Names
import io.fd.honeycomb.data.ModifiableDataManager
import io.fd.honeycomb.data.init.DataTreeInitializer
import io.fd.honeycomb.infra.distro.data.BindingDataBrokerProvider
import io.fd.honeycomb.infra.distro.data.DataTreeProvider
import io.fd.honeycomb.infra.distro.data.PersistingDataTreeProvider
import io.fd.honeycomb.infra.distro.initializer.PersistedFileInitializerProvider
import io.fd.honeycomb.translate.MappingContext
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree

class ContextPipelineModule extends PrivateModule {

    protected void configure() {
        // Bind also without annotation for easy private injection

        // Non persisting data tree
        def noPersistDataTreeProvider = new DataTreeProvider.ContextDataTreeProvider()
        bind(DataTree)
                .annotatedWith(Names.named("honeycomb-context-nopersist"))
                .toProvider(noPersistDataTreeProvider)
                .in(Singleton)
        expose(DataTree).annotatedWith(Names.named("honeycomb-context-nopersist"))
        // Persisting data tree wrapper
        def dataTreeProvider = new PersistingDataTreeProvider.ContextPersistingDataTreeProvider()
        bind(DataTree).toProvider(dataTreeProvider).in(Singleton)
//        bind(DataTree).annotatedWith(Names.named("honeycomb-context")).toProvider(dataTreeProvider).in(Singleton)
//        expose(DataTree).annotatedWith(Names.named("honeycomb-context"))

        bind(ModifiableDataManager).toProvider(ModifiableDTMgrProvider).in(Singleton)

        def domBrokerProvider = new HoneycombContextDOMDataBrokerProvider()
//        bind(DOMDataBroker).annotatedWith(Names.named("honeycomb-context")).toProvider(domBrokerProvider).in(Singleton)
        // Bind also without annotation for easy private injection
        bind(DOMDataBroker).toProvider(domBrokerProvider).in(Singleton)
//        expose(DOMDataBroker).annotatedWith(Names.named("honeycomb-context"))

        bind(DataBroker).annotatedWith(Names.named("honeycomb-context")).toProvider(BindingDataBrokerProvider).in(Singleton)
        expose(DataBroker).annotatedWith(Names.named("honeycomb-context"))

        bind(DataTreeInitializer)
                .annotatedWith(Names.named("honeycomb-context"))
                .toProvider(PersistedFileInitializerProvider.PersistedContextInitializerProvider)
                .in(Singleton)
        expose(DataTreeInitializer).annotatedWith(Names.named("honeycomb-context"))

        bind(MappingContext)
                .annotatedWith(Names.named("honeycomb-context"))
                .toProvider(RealtimeMappingContextProvider)
                .in(Singleton.class)
        expose(MappingContext).annotatedWith(Names.named("honeycomb-context"))
    }

}
