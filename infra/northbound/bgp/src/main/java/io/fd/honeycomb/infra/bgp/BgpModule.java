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

package io.fd.honeycomb.infra.bgp;

import static io.fd.honeycomb.infra.distro.data.InmemoryDOMDataBrokerProvider.CONFIG;
import static io.fd.honeycomb.infra.distro.data.InmemoryDOMDataBrokerProvider.OPERATIONAL;

import com.google.inject.PrivateModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.fd.honeycomb.infra.distro.data.BindingDataBrokerProvider;
import io.fd.honeycomb.infra.distro.data.DataStoreProvider;
import io.fd.honeycomb.infra.distro.data.InmemoryDOMDataBrokerProvider;
import io.fd.honeycomb.translate.bgp.RibWriter;
import io.netty.channel.EventLoopGroup;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.protocol.bgp.openconfig.impl.BGPOpenConfigMappingServiceImpl;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.BgpNeighbors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpModule extends PrivateModule {
    private static final Logger LOG = LoggerFactory.getLogger(BgpModule.class);

    static final String HONEYCOMB_BGP = "honeycomb-bgp";

    protected void configure() {
        LOG.debug("Initializing BgpModule");
        // Create BGPDispatcher BGPDispatcher for creating BGP clients
        bind(EventLoopGroup.class).toProvider(BgpNettyThreadGroupProvider.class).in(Singleton.class);
        bind(BGPDispatcher.class).toProvider(BGPDispatcherImplProvider.class).in(Singleton.class);

        configureRIB();

        // Configure peer registry
        bind(BGPOpenConfigMappingService.class).toInstance(new BGPOpenConfigMappingServiceImpl());
        bind(BGPPeerRegistry.class).toInstance(StrictBGPPeerRegistry.instance());

        // Create BGP server instance (initialize eagerly to start BGP)
        bind(BgpServerProvider.BgpServer.class).toProvider(BgpServerProvider.class).asEagerSingleton();

        // Initialize BgpNeighbours (initialize eagerly to start BGP neighbours)
        bind(BgpNeighbors.class).toProvider(BgpNeighboursProvider.class).asEagerSingleton();

        // Listens for local RIB modifications and passes routes to translation layer
        // (initialize eagerly to configure RouteWriters)
        bind(RibWriter.class).toProvider(LocRibWriterProvider.class).asEagerSingleton();
        expose(RibWriter.class);

        // install other BGP modules (hidden from HC user):
        install(new BgpConfigurationModule());
        install(new BgpExtensionsModule());
    }

    private void configureRIB() {
        // Create inmemory config data store for HONEYCOMB_BGP
        bind(InMemoryDOMDataStore.class).annotatedWith(Names.named(CONFIG))
            .toProvider(new DataStoreProvider(CONFIG, LogicalDatastoreType.CONFIGURATION))
            .in(Singleton.class);

        // Create inmemory operational data store for HONEYCOMB_BGP
        bind(InMemoryDOMDataStore.class).annotatedWith(Names.named(OPERATIONAL))
            .toProvider(new DataStoreProvider(OPERATIONAL, LogicalDatastoreType.OPERATIONAL))
            .in(Singleton.class);

        // Wrap datastores as DOMDataBroker
        // TODO make executor service configurable
        bind(DOMDataBroker.class).toProvider(InmemoryDOMDataBrokerProvider.class).in(Singleton.class);

        // Wrap DOMDataBroker as BA data broker (required by BgpReaderFactoryProvider)
        bind(DataBroker.class).annotatedWith(Names.named(HONEYCOMB_BGP)).toProvider(BindingDataBrokerProvider.class)
            .in(Singleton.class);
        expose(DataBroker.class).annotatedWith(Names.named(HONEYCOMB_BGP));

        // Create RIB instance
        bind(RIB.class).toProvider(BgpRIBProvider.class).in(Singleton.class);
    }
}
