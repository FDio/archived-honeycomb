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
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRibBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class BgpReaderFactoryProvider extends ProviderTrait<ReaderFactory> {

    @Inject
    @Named(BgpModule.HONEYCOMB_BGP)
    private DataBroker bgpDataBroker;

    @Override
    protected BgpReaderFactory create() {
        return new BgpReaderFactory(bgpDataBroker);
    }

    /**
     * {@link ReaderFactory} provides reader form BGP's dedicated data store.
     * Makes BGP operational data available over NETCONF/RESTCONF.
     */
    private static final class BgpReaderFactory implements ReaderFactory {

        private final DataBroker bgpDataBroker;

        BgpReaderFactory(final DataBroker bgpDataBroker) {
            this.bgpDataBroker = bgpDataBroker;
        }

        @Override
        public void init(final ModifiableReaderRegistryBuilder registry) {
            registry.add(new BindingBrokerReader<>(InstanceIdentifier.create(BgpRib.class),
                bgpDataBroker, LogicalDatastoreType.OPERATIONAL, BgpRibBuilder.class));
        }
    }
}
