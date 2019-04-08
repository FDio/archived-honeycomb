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

import static io.fd.honeycomb.northbound.bgp.extension.AbstractBgpExtensionModule.TABLES_IID;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.util.write.LegacyBindingBrokerWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.l3vpn.ipv4.routes.VpnIpv4Routes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L3VpnV4WriterFactory implements WriterFactory {

    private static final InstanceIdentifier<VpnIpv4Routes> V4_ROUTES_IID = TABLES_IID.child((Class) VpnIpv4Routes.class);

    @Inject
    @Named("honeycomb-bgp")
    private DataBroker dataBroker;

    @Override
    public void init(@Nonnull ModifiableWriterRegistryBuilder registry) {
        registry.wildcardedSubtreeAdd(new LegacyBindingBrokerWriter<>(V4_ROUTES_IID, dataBroker));
    }
}
