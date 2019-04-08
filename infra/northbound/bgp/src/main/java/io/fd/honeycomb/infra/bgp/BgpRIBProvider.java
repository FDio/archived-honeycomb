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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.broker.impl.PingPongDataBroker;
import org.opendaylight.controller.sal.core.compat.LegacyDOMDataBrokerAdapter;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.BGPRibRoutingPolicyFactoryImpl;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.CodecsRegistryImpl;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BgpRIBProvider extends ProviderTrait<RIB> {
    private static final Logger LOG = LoggerFactory.getLogger(BgpRIBProvider.class);

    @Inject
    private BgpConfiguration cfg;
    @Inject
    private BgpPolicyConfiguration policyCfg;
    @Inject
    private RIBExtensionConsumerContext extensions;
    @Inject
    private BGPDispatcher dispatcher;
    @Inject
    private BindingToNormalizedNodeCodec codec;
    @Inject
    private DOMDataBroker domBroker;
    @Inject
    @Named(BgpModule.HONEYCOMB_BGP)
    private DataBroker dataBroker;
    @Inject
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Inject
    private DOMSchemaService schemaService;
    @Inject
    private ShutdownHandler shutdownHandler;
    @Inject
    private Set<AfiSafi> configuredAfiSafis;

    @Override
    protected RIB create() {

        LegacyDOMDataBrokerAdapter DomDataBrokerAdapter = new LegacyDOMDataBrokerAdapter(domBroker);
        DomDataBrokerAdapter.getSupportedExtensions().get(DOMSchemaService.class);

        Preconditions.checkArgument(policyCfg.getPolicyConfig().isPresent(),
                "Bgp policy configuration failed to load. Check bgp-policy.json configuration file.");
        final AsNumber asNumber = new AsNumber(cfg.bgpAsNumber.get().longValue());
        final Ipv4Address routerId = new Ipv4Address(cfg.bgpBindingAddress.get());
        final ClusterIdentifier clusterId = new ClusterIdentifier(routerId);
        LOG.debug("Creating BGP RIB: routerId={}, asNumber={}", routerId, asNumber);
        // TODO(HONEYCOMB-395): should all afi-safis use the same send-max value?
        // TODO(HONEYCOMB-363): configure other BGP Multiprotocol extensions:

        final ArrayList<AfiSafi> afiSafiList = new ArrayList<>(configuredAfiSafis);
        // based on org.opendaylight.protocol.bgp.rib.impl.config.RibImpl.createRib
        final PingPongDataBroker pingPongDataBroker = new PingPongDataBroker(DomDataBrokerAdapter);
        final CodecsRegistryImpl codecsRegistry =
                CodecsRegistryImpl.create(codec, extensions.getClassLoadingStrategy());

        final BGPRibRoutingPolicyFactoryImpl bgpRibRoutingPolicyFactory =
                new BGPRibRoutingPolicyFactoryImpl(dataBroker, new StatementRegistry());
        final BGPRibRoutingPolicy ribPolicies = bgpRibRoutingPolicyFactory
                .buildBGPRibPolicy(cfg.bgpAsNumber.get(), new Ipv4Address(cfg.bgpBindingAddress.get()), clusterId,
                        policyCfg.getPolicyConfig().get());
        final RIBImpl rib = new RIBImpl(tableTypeRegistry, new RibId(cfg.bgpProtocolInstanceName.get()),
                asNumber, new BgpId(routerId), extensions, dispatcher, codecsRegistry, pingPongDataBroker, dataBroker,
                ribPolicies, toTableTypes(afiSafiList, tableTypeRegistry), Collections.emptyMap());
        rib.instantiateServiceInstance();

        // required for proper RIB's CodecRegistry initialization (based on RIBImpl.start)
        schemaService.registerSchemaContextListener(rib);
        shutdownHandler.register("ping-pong-data-broker", pingPongDataBroker);
        LOG.debug("BGP RIB created successfully: {}", rib);
        return rib;
    }

    private List<BgpTableType> toTableTypes(final List<AfiSafi> afiSafis,
                                           final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        return afiSafis.stream()
                .filter(afiSafi -> afiSafi.getAfiSafiName()!=null)
                .map(afiSafi -> tableTypeRegistry.getTableType(afiSafi.getAfiSafiName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
