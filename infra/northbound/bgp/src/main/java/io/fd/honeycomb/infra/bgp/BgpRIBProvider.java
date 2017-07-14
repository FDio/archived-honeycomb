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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BgpRIBProvider extends ProviderTrait<RIB> {
    private static final Logger LOG = LoggerFactory.getLogger(BgpRIBProvider.class);

    @Inject
    private BgpConfiguration cfg;
    @Inject
    private RIBExtensionConsumerContext extensions;
    @Inject
    private BGPDispatcher dispatcher;
    @Inject
    private BindingToNormalizedNodeCodec codec;
    @Inject
    private DOMDataBroker domBroker;
    @Inject
    private BGPOpenConfigMappingService mappingService;
    @Inject
    private SchemaService schemaService;

    @Override
    protected RIB create() {
        final AsNumber asNumber = new AsNumber(cfg.bgpAsNumber.get().longValue());
        final Ipv4Address routerId = new Ipv4Address(cfg.bgpBindingAddress.get());
        final ClusterIdentifier clusterId = new ClusterIdentifier(routerId);
        LOG.debug("Creating BGP RIB: routerId={}, asNumber={}", routerId, asNumber);
        // TODO configure other BGP Multiprotocol extensions:
        final List<AfiSafi> afiSafi = ImmutableList.of(
            new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
            .addAugmentation(AfiSafi2.class,
                new AfiSafi2Builder().setReceive(cfg.isBgpMultiplePathsEnabled())
                    .setSendMax(cfg.bgpSendMaxMaths.get().shortValue()).build())
            .build(),
            new AfiSafiBuilder().setAfiSafiName(IPV4LABELLEDUNICAST.class)
                .addAugmentation(AfiSafi2.class,
                    new AfiSafi2Builder().setReceive(cfg.isBgpMultiplePathsEnabled())
                        .setSendMax(cfg.bgpSendMaxMaths.get().shortValue()).build())
                .build()
            );
        final Map<TablesKey, PathSelectionMode> pathSelectionModes = mappingService.toPathSelectionMode(afiSafi)
            .entrySet().stream().collect(Collectors.toMap(entry ->
                new TablesKey(entry.getKey().getAfi(), entry.getKey().getSafi()), Map.Entry::getValue));
        // based on RIBImpl.createRib
        final RIBImpl rib =
            new RIBImpl(new NoopClusterSingletonServiceProvider(), new RibId(cfg.bgpProtocolInstanceName.get()),
                asNumber, new BgpId(routerId), clusterId, extensions, dispatcher, codec,
                new PingPongDataBroker(domBroker), mappingService.toTableTypes(afiSafi), pathSelectionModes,
                extensions.getClassLoadingStrategy(), null);

        // required for proper RIB's CodecRegistry initialization (based on RIBImpl.start)
        schemaService.registerSchemaContextListener(rib);

        LOG.debug("BGP RIB created successfully: {}", rib);
        return rib;
    }

    /**
     * HC does not support clustering, but BGP uses {@link ClusterSingletonServiceProvider}
     * to initialize {@link RIBImpl}. Therefore we provide this dummy implementation.
     */
    private static final class NoopClusterSingletonServiceProvider implements ClusterSingletonServiceProvider {
        private static final Logger LOG = LoggerFactory.getLogger(NoopClusterSingletonServiceProvider.class);

        private static final ClusterSingletonServiceRegistration REGISTRATION =
            () -> LOG.debug("Closing ClusterSingletonServiceRegistration");

        @Override
        public ClusterSingletonServiceRegistration registerClusterSingletonService(
            final ClusterSingletonService clusterSingletonService) {
            clusterSingletonService.instantiateServiceInstance();
            return REGISTRATION;
        }

        @Override
        public void close() throws Exception {
            LOG.debug("Closing NoopClusterSingletonServiceProvider");
        }
    }
}
