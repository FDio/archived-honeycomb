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

package io.fd.honeycomb.bgp.translate.impl;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import io.fd.honeycomb.translate.bgp.RouteWriter;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class LocRibWriterTest {

    private static final InstanceIdentifier<Tables> TABLES = InstanceIdentifier.create(BgpRib.class).child(Rib.class)
        .child(LocRib.class).child(Tables.class);

    @SuppressWarnings("unchecked")
    private static final KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey>
        SPECIFIC_IP4_ROUTE_ID =
        InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(new RibId("some-rib"))).child(LocRib.class)
            .child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class))
            .child((Class) Ipv4Routes.class)
            .child(Ipv4Route.class, new Ipv4RouteKey(new PathId(1L), new Ipv4Prefix("1.2.3.4/24")));

    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<Ipv4Route> IP4_ROUTE_ID =
        TABLES.child((Class) Ipv4Routes.class).child(Ipv4Route.class);

    @SuppressWarnings("unchecked")
    private static final InstanceIdentifier<LabeledUnicastRoute> LABELED_IP4_ID =
        TABLES.child((Class) LabeledUnicastRoutes.class).child(LabeledUnicastRoute.class);

    @Mock
    private DataBroker bgpDataBroker;
    private LocRibWriter ribWriter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ribWriter = new LocRibWriter(bgpDataBroker);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterFailsForNonRoute() {
        ribWriter.register(new NoopWriter(IP4_ROUTE_ID.child(Attributes.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterFailsForNonLocRibRoute() {
        ribWriter.register(new NoopWriter(InstanceIdentifier.create(Ipv4Route.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterFailsForSpecificLocRibRoute() {
        ribWriter.register(new NoopWriter(SPECIFIC_IP4_ROUTE_ID));
    }

    @Test
    public void testRegisterIpv4RouteWriter() {
        ribWriter.register(new NoopWriter(IP4_ROUTE_ID));
        Mockito.verify(bgpDataBroker).registerDataTreeChangeListener(
            ArgumentMatchers.eq(new DataTreeIdentifier<>(OPERATIONAL, IP4_ROUTE_ID)), ArgumentMatchers.any());
    }

    @Test
    public void testRegisterLabeledIpv4RouteWriter() {
        ribWriter.register(new NoopWriter(LABELED_IP4_ID));
        Mockito.verify(bgpDataBroker).registerDataTreeChangeListener(
            ArgumentMatchers.eq(new DataTreeIdentifier<>(OPERATIONAL, LABELED_IP4_ID)), ArgumentMatchers.any());
    }

    private static final class NoopWriter implements RouteWriter {
        private final InstanceIdentifier<? extends Route> id;

        private NoopWriter(@Nonnull final InstanceIdentifier id) {
            this.id = id;
        }

        @SuppressWarnings("unchecked")
        @Nonnull
        @Override
        public InstanceIdentifier getManagedDataObjectType() {
            return id;
        }

        @Override
        public void create(@Nonnull final InstanceIdentifier id, @Nullable final Route dataAfter)
            throws WriteFailedException.CreateFailedException {
        }

        @Override
        public void delete(@Nonnull final InstanceIdentifier id, @Nullable final Route dataBefore)
            throws WriteFailedException.DeleteFailedException {
        }

        @Override
        public void update(@Nonnull final InstanceIdentifier id, @Nullable final Route dataBefore,
                           @Nullable final Route dataAfter) throws WriteFailedException.UpdateFailedException {
        }
    }
}