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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.util.write.BindingBrokerWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * {@link WriterFactory} for BGP Application RIB write integration with HC writer registry.
 * Uses BindingBrokerWriter to write routes via dedicated broker that, unlike
 * {@link io.fd.honeycomb.data.impl.DataBroker DataBroker}, supports tx chains and DOMDataChangeListener registration
 * extensively used by ODL's bgp.
 * <p>
 * As a bonus BGP routes persisted and available for read via RESTCONF/NETCONF.
 */
final class ApplicationRibWriterFactory implements WriterFactory {

    @Inject
    @Named(BgpModule.HONEYCOMB_BGP)
    private DataBroker dataBroker;

    private static final InstanceIdentifier<ApplicationRib> AR_IID =
            InstanceIdentifier.create(ApplicationRib.class);
    private static final InstanceIdentifier<Tables> TABLES_IID = AR_IID.child(Tables.class);


    // TODO (HONEYCOMB-359):
    // BGP models are huge, we need some kind of wildcarded subtree writer, that works for whole subtree.
    // 1) we can either move checking handledTypes to writers (getHandledTypes, isAffected, writer.getHandedTypes, ...)
    // but then precondition check in flatWriterRegistry might be slower (we need to check if we have all writers
    // in order to avoid unnecessary reverts).
    //
    // 2) alternative is to compute all child nodes during initialization (might introduce some footprint penalty).
    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        registry.subtreeAdd(ImmutableSet.of(TABLES_IID), new BindingBrokerWriter<>(InstanceIdentifier.create(ApplicationRib.class), dataBroker)
        );
    }
}
