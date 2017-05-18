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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.translate.util.JsonUtils.readContainerEntryJson;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.APPLICATION_PEER_GROUP_NAME;
import static org.opendaylight.yangtools.sal.binding.generator.impl.BindingSchemaContextUtils.findDataNodeContainer;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.config.AppPeer;
import org.opendaylight.protocol.bgp.rib.impl.config.BgpPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.BgpNeighborPeerGroupConfig;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BGPPeerRegistryProvider extends ProviderTrait<BGPPeerRegistry> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerRegistryProvider.class);
    private static final String PEERS_CFG = "/bgp-peers.json";
    @Inject
    private BindingToNormalizedNodeCodec codec;
    @Inject
    private RIB globalRib;
    @Inject
    private BGPOpenConfigMappingService mappingService;
    @Inject
    private SchemaService schemaService;

    @Override
    protected BGPPeerRegistry create() {
        final BGPPeerRegistry peerRegistry = StrictBGPPeerRegistry.instance();
        final Neighbors neighbors = readNeighbours();
        for (final Neighbor neighbor : neighbors.getNeighbor()) {
            if (isApplicationPeer(neighbor)) {
                LOG.trace("Starting AppPeer for {}", neighbor);
                new AppPeer().start(globalRib, neighbor, mappingService, null);
            } else {
                LOG.trace("Starting BgpPeer for {}", neighbor);
                new BgpPeer(null, peerRegistry).start(globalRib, neighbor, mappingService, null);
            }
        }
        LOG.debug("Created BGPPeerRegistry with neighbours {}", neighbors);
        return peerRegistry;
    }

    private Neighbors readNeighbours() {
        LOG.debug("Reading BGP neighbours from {}", PEERS_CFG);
        final InputStream resourceStream = this.getClass().getResourceAsStream(PEERS_CFG);
        checkState(resourceStream != null, "Resource %s not found", PEERS_CFG);

        final InstanceIdentifier<Bgp> bgpII = InstanceIdentifier.create(NetworkInstances.class)
            .child(NetworkInstance.class, new NetworkInstanceKey("dummy-value")).child(Protocols.class)
            .child(Protocol.class, new ProtocolKey(BGP.class, "dummy-value")).augmentation(Protocol1.class)
            .child(Bgp.class);
        final InstanceIdentifier<Neighbors> neighborsII = bgpII.child(Neighbors.class);

        final YangInstanceIdentifier neighborsYII = codec.toYangInstanceIdentifier(neighborsII);
        final SchemaContext schemaContext = schemaService.getGlobalContext();
        final Optional<DataNodeContainer> parentNode = findDataNodeContainer(schemaContext, bgpII);
        final ContainerNode parentContainer = readContainerEntryJson(schemaContext, resourceStream,
            (SchemaNode) parentNode.get(),
            (YangInstanceIdentifier.NodeIdentifier) neighborsYII.getLastPathArgument());
        final NormalizedNode<?, ?> neighborsContainer = parentContainer.getValue().iterator().next();

        final Map.Entry<InstanceIdentifier<?>, DataObject> entry = codec.fromNormalizedNode(neighborsYII, neighborsContainer);
        checkNotNull(entry, "Failed to deserialize neighbours configuration at %s", PEERS_CFG);
        return (Neighbors) entry.getValue();
    }

    private static boolean isApplicationPeer(@Nonnull final Neighbor neighbor) {
        return java.util.Optional.of(neighbor.getConfig())
            .map(config -> config.getAugmentation(Config2.class))
            .map(BgpNeighborPeerGroupConfig::getPeerGroup)
            .map(APPLICATION_PEER_GROUP_NAME::equals)
            .orElse(false);
    }
}
