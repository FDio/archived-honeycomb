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

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.toTableTypes;

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
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Inject
    private SchemaService schemaService;
    @Inject
    private ShutdownHandler shutdownHandler;
    @Inject
    private Set<AfiSafi> configuredAfiSafis;

    @Override
    protected RIB create() {
        final AsNumber asNumber = new AsNumber(cfg.bgpAsNumber.get().longValue());
        final Ipv4Address routerId = new Ipv4Address(cfg.bgpBindingAddress.get());
        final ClusterIdentifier clusterId = new ClusterIdentifier(routerId);
        LOG.debug("Creating BGP RIB: routerId={}, asNumber={}", routerId, asNumber);
        // TODO(HONEYCOMB-395): should all afi-safis use the same send-max value?
        // TODO(HONEYCOMB-363): configure other BGP Multiprotocol extensions:

        final ArrayList<AfiSafi> afiSafiList = new ArrayList<>(configuredAfiSafis);
        final Map<TablesKey, PathSelectionMode> pathSelectionModes =
                OpenConfigMappingUtil.toPathSelectionMode(afiSafiList, tableTypeRegistry)
                        .entrySet().stream().collect(Collectors.toMap(entry ->
                        new TablesKey(entry.getKey().getAfi(), entry.getKey().getSafi()), Map.Entry::getValue));
        // based on org.opendaylight.protocol.bgp.rib.impl.config.RibImpl.createRib
        final PingPongDataBroker pingPongDataBroker = new PingPongDataBroker(domBroker);
        final RIBImpl rib =
                new RIBImpl(new NoopClusterSingletonServiceProvider(), new RibId(cfg.bgpProtocolInstanceName.get()),
                        asNumber, new BgpId(routerId), clusterId, extensions, dispatcher, codec,
                        pingPongDataBroker, toTableTypes(afiSafiList, tableTypeRegistry), pathSelectionModes,
                        extensions.getClassLoadingStrategy(), null);

        // required for proper RIB's CodecRegistry initialization (based on RIBImpl.start)
        schemaService.registerSchemaContextListener(rib);
        shutdownHandler.register("ping-pong-data-broker", pingPongDataBroker);
        LOG.debug("BGP RIB created successfully: {}", rib);
        return rib;
    }

    /**
     * HC does not support clustering, but BGP uses {@link ClusterSingletonServiceProvider} to initialize {@link
     * RIBImpl}. Therefore we provide this dummy implementation.
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
