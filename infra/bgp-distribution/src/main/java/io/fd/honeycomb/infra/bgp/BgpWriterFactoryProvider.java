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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import io.fd.honeycomb.translate.util.write.BindingBrokerWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHop;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class BgpWriterFactoryProvider extends ProviderTrait<WriterFactory> {
    @Inject
    @Named(BgpModule.HONEYCOMB_BGP)
    private DataBroker bgpDataBroker;
    @Override
    protected BgpWriterFactory create() {
        return new BgpWriterFactory(bgpDataBroker);
    }

    /**
     * {@link WriterFactory} for BGP cfg write integration with HC writer registry.
     * Using BindingBrokerWriter to write BGP configuration data via dedicated broker that, unlike
     * {@link io.fd.honeycomb.data.impl.DataBroker}, supports tx chains and DOMDataChangeListener registration
     * extensively used by ODL's bgp.
     *
     * As a bonus BGP route configuration is persisted and available for read via RESTCONF/NETCONF.
     */
    private static final class BgpWriterFactory implements WriterFactory {
        private final DataBroker dataBroker;

        private static final InstanceIdentifier<ApplicationRib> AR_IID =
            InstanceIdentifier.create(ApplicationRib.class);
        private static final InstanceIdentifier<Tables> TABLES_IID = AR_IID.child(Tables.class);
        private static final InstanceIdentifier<Ipv4Routes> IPV4_ROUTES_IID = TABLES_IID.child((Class) Ipv4Routes.class);
        private static final InstanceIdentifier<Ipv4Route> IPV4_ROUTE_IID = IPV4_ROUTES_IID.child(Ipv4Route.class);

        private BgpWriterFactory(final DataBroker dataBroker) {
            this.dataBroker = dataBroker;
        }

        // TODO (HONEYCOMB-359):
        // BGP models are huge, we need some kind of wildcarded subtree writer, that works for whole subtree.
        // 1) we can either move checking handledTypes to writers (getHandledTypes, isAffected, writer.getHandedTypes, ...)
        // but then precondition check in flatWriterRegistry might be slower (we need to check if we have all writers
        // in order to avoid unnecessary reverts).
        //
        // 2) alternative is to compute all child nodes during initialization (might introduce some footprint penalty).
        @Override
        public void init(final ModifiableWriterRegistryBuilder registry) {
            registry.subtreeAdd(
                Sets.newHashSet(
                    TABLES_IID,
                    IPV4_ROUTES_IID,
                    IPV4_ROUTES_IID.child(Ipv4Route.class),
                    IPV4_ROUTE_IID.child(Attributes.class),
                    IPV4_ROUTE_IID.child(Attributes.class).child(Origin.class),
                    IPV4_ROUTE_IID.child(Attributes.class).child(LocalPref.class),
                    IPV4_ROUTE_IID.child(Attributes.class).child(Ipv4NextHop.class)
                ),
                new BindingBrokerWriter<>(InstanceIdentifier.create(ApplicationRib.class), dataBroker)
            );
        }
    }
}
