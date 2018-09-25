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

package io.fd.honeycomb.infra.bgp.neighbors;

import com.google.inject.Inject;
import io.fd.honeycomb.infra.bgp.BgpConfiguration;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;

/**
 * Initializes writer for Bgp Neighbors ({@link Neighbor} node) and all its parents required by HC infra.
 */
public final class BgpPeerWriterFactory implements WriterFactory {
    private static final InstanceIdentifier<NetworkInstance> NETWORK_INSTANCE_ID =
            InstanceIdentifier.create(NetworkInstances.class)
                    .child(NetworkInstance.class);

    private static final InstanceIdentifier<Protocol> PROTOCOL_ID =
            NETWORK_INSTANCE_ID.child(Protocols.class).child(Protocol.class);

    private static final InstanceIdentifier<Neighbor> NEIGHBOR_ID =
            PROTOCOL_ID.augmentation(NetworkInstanceProtocol.class).child(Bgp.class).child(Neighbors.class)
                    .child(Neighbor.class);

    @Inject
    private BgpConfiguration configuration;
    @Inject
    private RIB globalRib;
    @Inject
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Inject
    private BGPPeerRegistry peerRegistry;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // NetworkInstances
        //  NetworkInstance =
        registry.add(new GenericListWriter<>(NETWORK_INSTANCE_ID,
                new NetworkInstanceCustomizer(configuration.bgpNetworkInstanceName)));

        //   Protocols
        //    Protocol =
        registry.add(
                new GenericListWriter<>(PROTOCOL_ID, new ProtocolCustomizer(configuration.bgpProtocolInstanceName.get())));

        //     Protocol1 augmentation (from bgp-openconfig-extensions)
        //      Bgp
        //       Neighbors
        //        Neighbor=
        registry.wildcardedSubtreeAdd(new GenericListWriter<>(NEIGHBOR_ID, new NeighborCustomizer(globalRib, peerRegistry,
                tableTypeRegistry, configuration)));
    }
}

