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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.APPLICATION_PEER_GROUP_NAME;

import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.rib.impl.config.PeerBean;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2Builder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NeighborCustomizerTest {
    private static final IpAddress IP = new IpAddress(new Ipv4Address("10.25.1.9"));
    private static final InstanceIdentifier<Neighbor> ID =
        InstanceIdentifier.create(Neighbors.class).child(Neighbor.class, new NeighborKey(IP));

    @Mock
    private RIB globalRib;
    @Mock
    private BGPPeerRegistry peerRegistry;
    @Mock
    private BGPOpenConfigMappingService mappingService;
    @Mock
    private WriteContext ctx;

    private NeighborCustomizer customizer;

    @Before
    public void setUp() {
        initMocks(this);
        when(globalRib.getYangRibId()).thenReturn(YangInstanceIdentifier.EMPTY);
        when(globalRib.getRibIServiceGroupIdentifier()).thenReturn(ServiceGroupIdentifier.create("sgid"));
        customizer = new NeighborCustomizer(globalRib, peerRegistry, mappingService);
    }

    @Test
    public void testAddAppPeer() throws WriteFailedException {
        final Neighbor neighbor = new NeighborBuilder()
            .setNeighborAddress(IP)
            .setConfig(
                new ConfigBuilder()
                    .addAugmentation(
                        Config2.class,
                        new Config2Builder().setPeerGroup(APPLICATION_PEER_GROUP_NAME).build()
                    ).build())
            .build();
        customizer.writeCurrentAttributes(ID, neighbor, ctx);
        assertTrue(customizer.isPeerConfigured(ID));
    }

    @Test
    public void testAddInternalPeer() throws WriteFailedException {
        final Neighbor neighbor = new NeighborBuilder()
            .setNeighborAddress(IP)
            .setConfig(
                new ConfigBuilder()
                    .setPeerType(PeerType.INTERNAL)
                    .build())
            .build();
        customizer.writeCurrentAttributes(ID, neighbor, ctx);
        assertTrue(customizer.isPeerConfigured(ID));
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        final PeerBean peer = mock(PeerBean.class);
        customizer.addPeer(ID, peer);
        final Neighbor before = mock(Neighbor.class);
        final Neighbor after = mock(Neighbor.class);
        customizer.updateCurrentAttributes(ID, before, after, ctx);
        verify(peer).closeServiceInstance();
        verify(peer).close();
        verify(peer).start(globalRib, after, mappingService, null);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        final PeerBean peer = mock(PeerBean.class);
        customizer.addPeer(ID, peer);
        final Neighbor before = mock(Neighbor.class);
        customizer.deleteCurrentAttributes(ID, before, ctx);
        verify(peer).closeServiceInstance();
        verify(peer).close();
        assertFalse(customizer.isPeerConfigured(ID));
    }
}