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

package io.fd.honeycomb.northbound.bgp.extension;

import static io.fd.honeycomb.northbound.bgp.extension.AbstractBgpExtensionModule.TableTypeRegistration.tableType;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.fd.honeycomb.infra.bgp.BgpConfiguration;
import io.fd.honeycomb.translate.write.WriterFactory;
import java.util.Set;
import org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator;
import org.opendaylight.protocol.bgp.linkstate.impl.RIBActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.LINKSTATE;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAddPathsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAddPathsConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkstateModule extends AbstractBgpExtensionModule {

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateModule.class);

    @Override
    public Set<Class<? extends RIBExtensionProviderActivator>> getRibActivators() {
        return ImmutableSet.of(RIBActivator.class);
    }

    @Override
    public Set<Class<? extends BGPExtensionProviderActivator>> getExtensionActivators() {
        return ImmutableSet.of(BGPActivator.class);
    }

    @Override
    public Set<TableTypeRegistration> getTableTypes() {
        return ImmutableSet.of(
                tableType(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class, LINKSTATE.class));
    }

    @Override
    public Set<Class<? extends Provider<AfiSafi>>> getAfiSafiTypeProviders() {
        return ImmutableSet.of(LinkstateAfiSafiProvider.class);
    }

    @Override
    public Set<Class<? extends WriterFactory>> getApplicationRibWriters() {
        return ImmutableSet.of(LinkstateWriterFactory.class);
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }

    private static class LinkstateAfiSafiProvider implements Provider<AfiSafi> {

        @Inject
        private BgpConfiguration cfg;

        @Override
        public AfiSafi get() {
            return new AfiSafiBuilder().setAfiSafiName(LINKSTATE.class)
                    .addAugmentation(NeighborAddPathsConfig.class,
                            new NeighborAddPathsConfigBuilder().setReceive(cfg.isBgpMultiplePathsEnabled())
                                    .setSendMax(cfg.bgpSendMaxMaths.get().shortValue()).build())
                    .build();
        }
    }
}
