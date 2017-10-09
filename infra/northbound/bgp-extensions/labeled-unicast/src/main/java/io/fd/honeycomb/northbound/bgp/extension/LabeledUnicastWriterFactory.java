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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.util.write.BindingBrokerWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHop;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;

import static io.fd.honeycomb.northbound.bgp.extension.AbstractBgpExtensionModule.TABLES_IID;


public class LabeledUnicastWriterFactory implements WriterFactory {

    private static final InstanceIdentifier<LabeledUnicastRoutes> LABELED_UNICAST_ROUTES_IID = TABLES_IID.child((Class) LabeledUnicastRoutes.class);

    @Inject
    @Named("honeycomb-bgp")
    private DataBroker dataBroker;

    @Override
    public void init(@Nonnull ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<LabeledUnicastRoutes> subtreeIid = InstanceIdentifier.create(LabeledUnicastRoutes.class);

        //TODO - HONEYCOMB-359 - use wildcarded subtree writer
        registry.subtreeAdd(
                Sets.newHashSet(
                        subtreeIid.child(LabeledUnicastRoute.class),
                        subtreeIid.child(LabeledUnicastRoute.class).child(Attributes.class),
                        subtreeIid.child(LabeledUnicastRoute.class).child(Attributes.class).child(Origin.class),
                        subtreeIid.child(LabeledUnicastRoute.class).child(Attributes.class).child(LocalPref.class),
                        subtreeIid.child(LabeledUnicastRoute.class).child(Attributes.class).child(Ipv4NextHop.class),
                        subtreeIid.child(LabeledUnicastRoute.class).child(LabelStack.class)
                ),
                new BindingBrokerWriter<>(LABELED_UNICAST_ROUTES_IID, dataBroker));
    }
}
