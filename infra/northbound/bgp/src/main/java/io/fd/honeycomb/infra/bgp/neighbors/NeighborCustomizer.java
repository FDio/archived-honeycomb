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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import io.fd.honeycomb.infra.bgp.BgpConfiguration;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.config.AppPeer;
import org.opendaylight.protocol.bgp.rib.impl.config.BgpPeer;
import org.opendaylight.protocol.bgp.rib.impl.config.PeerBean;
import org.opendaylight.protocol.bgp.rib.impl.config.PeerGroupConfigLoader;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborPeerGroupConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer responsible for management of BGP Neighbors. Partially based on BgpDeployerImpl from ODL's BGP (was hard to
 * use directly due to OSGI dependencies).
 */
@ThreadSafe
final class NeighborCustomizer implements ListWriterCustomizer<Neighbor, NeighborKey> {
    private static final Logger LOG = LoggerFactory.getLogger(NeighborCustomizer.class);
    private final RIB globalRib;
    private final BGPPeerRegistry peerRegistry;
    private final PeerGroupConfigLoader peerGroupLoader;
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @VisibleForTesting
    static final InstanceIdentifier<Bgp> bgpIid = InstanceIdentifier.create(NetworkInstances.class)
            .child(NetworkInstance.class).child(Protocols.class).child(Protocol.class).augmentation(
                    NetworkInstanceProtocol.class).child(Bgp.class);


    @GuardedBy("this")
    private final Map<InstanceIdentifier<Neighbor>, PeerBean> peers = new HashMap<>();

    public NeighborCustomizer(@Nonnull final RIB globalRib, @Nonnull final BGPPeerRegistry peerRegistry,
                              @Nonnull final BGPTableTypeRegistryConsumer tableTypeRegistry,
                              final BgpConfiguration configuration) {
        this.globalRib = checkNotNull(globalRib, "globalRib should not be null");
        this.peerRegistry = checkNotNull(peerRegistry, "peerRegistry should not be null");
        this.tableTypeRegistry = checkNotNull(tableTypeRegistry, "tableTypeRegistry should not be null");
        this.peerGroupLoader = checkNotNull(configuration, "configuration should not be null");
    }

    @VisibleForTesting
    synchronized void addPeer(@Nonnull final InstanceIdentifier<Neighbor> id,
                              @Nonnull final PeerBean peer) {
        peers.put(id, peer);
    }

    @VisibleForTesting
    synchronized boolean isPeerConfigured(@Nonnull final InstanceIdentifier<Neighbor> id) {
        return peers.containsKey(id);
    }

    @Override
    public synchronized void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Neighbor> id,
                                                    @Nonnull final Neighbor neighbor,
                                                    @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final PeerBean peer;
        if (isAppPeer(neighbor)) {
            LOG.debug("Creating AppPeer bean for {}: {}", id, neighbor);
            peer = new AppPeer();
        } else {
            LOG.debug("Starting BgpPeer bean for {}: {}", id, neighbor);
            peer = new BgpPeer(null);
        }
        LOG.debug("Starting bgp peer for {}", id);
        peer.start(globalRib, neighbor, bgpIid, peerGroupLoader, tableTypeRegistry);
        peer.instantiateServiceInstance();
        addPeer(id, peer);
    }

    static boolean isAppPeer(final Neighbor neighbor) {
        Config config = neighbor.getConfig();
        if (config != null) {
            NeighborPeerGroupConfig config1 = config.augmentation(NeighborPeerGroupConfig.class);
            if (config1 != null) {
                String peerGroup = config1.getPeerGroup();
                return peerGroup != null && peerGroup.equals("application-peers");
            }
        }
        return false;
    }

    @Override
    public synchronized void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Neighbor> id,
                                                     @Nonnull final Neighbor dataBefore,
                                                     @Nonnull final Neighbor dataAfter,
                                                     @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Updating Peer instance {} with configuration: {}", id, dataAfter);
        final PeerBean peer = peers.get(id);
        checkState(peer != null, "Could not find peer bean while updating neighbor {}", id);
        closePeerBean(peer);
        peer.start(globalRib, dataAfter, bgpIid, peerGroupLoader, tableTypeRegistry);
        peer.instantiateServiceInstance();
        LOG.debug("Peer instance updated {}", peer);
    }

    @Override
    public synchronized void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Neighbor> id,
                                                     @Nonnull final Neighbor dataBefore,
                                                     @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Removing Peer instance: {}", id);
        final PeerBean peer = peers.remove(id);
        if (peer != null) {
            closePeerBean(peer);
            LOG.debug("Peer instance removed {}", peer);
        }
    }

    private static void closePeerBean(final PeerBean peer) {
        try {
            peer.closeServiceInstance().get();
        } catch (final Exception e) {
            LOG.error("Peer instance failed to close service instance", e);
        }
        peer.close();
    }
}
